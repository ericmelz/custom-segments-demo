import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;

public class Demo1 {
    static StandardAnalyzer analyzer = new StandardAnalyzer();
    static Directory index = new ByteBuffersDirectory();
    static IndexWriterConfig config = new IndexWriterConfig();
    static IndexWriter w;
    static {
        try {
            w = new IndexWriter(index, config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static int searchCounter = 0;

    static void addDoc(IndexWriter w, String title, String isbn) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new StringField("isbn", isbn, Field.Store.YES));
        w.addDocument(doc);
    }

    void setupIndex() throws IOException {
        addDoc(w, "Lucene in Action", "1933398817");
        addDoc(w, "Lucene for Dummies", "55320055Z");
        addDoc(w, "Managing Gigabytes", "55063554A");
        addDoc(w, "The Art of Computer Science", "9900333X");
        w.close();
    }

    Query makeQuery(String querystr) throws ParseException {
        return new QueryParser("title", analyzer).parse(querystr);
    }

    IndexSearcher openSearcher(Query q) throws IOException {
        IndexReader reader = DirectoryReader.open(index);
        return new IndexSearcher(reader);
    }

    ScoreDoc[] search(IndexSearcher searcher, Query q) throws IOException {
        int hitsPerPage = 10;

        long start = System.nanoTime();
        TopDocs docs = searcher.search(q, hitsPerPage);
        long end = System.nanoTime();
        long durationMs = (end - start) / 1000000;
        System.out.println("duration " + searchCounter + " is " + durationMs);
        searchCounter++;

        return docs.scoreDocs;
    }

    void display(ScoreDoc[] hits, IndexSearcher searcher) throws IOException {
        System.out.println("Found " + hits.length + " hits.");
        for (int i=0; i < hits.length; i++) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            System.out.println((i+1) + ". " + d.get("isbn") + "\t" + d.get("title"));
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("hey");
        Demo1 demo1 = new Demo1();
        demo1.setupIndex();
        Query q = demo1.makeQuery("lucene");
        IndexSearcher searcher = demo1.openSearcher(q);
        ScoreDoc[] hits = demo1.search(searcher, q);
        demo1.display(hits, searcher);
        ScoreDoc[] hits2 = demo1.search(searcher, q);
        demo1.display(hits2, searcher);
    }
}

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Demo3 {
    /**
     * This demonstrates the use of Lucene to match verity documents (pages) with custom segments.
     * Custom Segments are stored in the Lucene index - a single Lucene document represents a Custom Segment
     * Custom segments are matched against pages by using the category and keyword values in a page to match
     * custom segments.
     *
     * Example:
     * Using string IAB categories (e.g., "i9") and 100K documents in the index, we see latencies of about 1ms:
     *
     * There are 100000 documents in the index.
     *
     * query=i8
     * match duration 0 is 17965 microseconds
     * Found 10 hits.
     * 1. 	Document 3	i8, i4
     * 2. 	Document 13	i6, i8, i2
     * 3. 	Document 23	i10, i8
     * 4. 	Document 31	i8
     * 5. 	Document 33	i8, i7, i1
     * 6. 	Document 47	i8, i10
     * 7. 	Document 56	i8
     * 8. 	Document 58	i8, i2
     * 9. 	Document 69	i9, i8, i2
     * 10. 	Document 76	i8, i10
     *
     * match duration 1 is 1058 microseconds
     * Found 10 hits.
     * 1. 	Document 3	i8, i4
     * 2. 	Document 13	i6, i8, i2
     * 3. 	Document 23	i10, i8
     * 4. 	Document 31	i8
     * 5. 	Document 33	i8, i7, i1
     * 6. 	Document 47	i8, i10
     * 7. 	Document 56	i8
     * 8. 	Document 58	i8, i2
     * 9. 	Document 69	i9, i8, i2
     * 10. 	Document 76	i8, i10
     *
     * match duration 2 is 973 microseconds
     * Found 10 hits.
     * 1. 	Document 3	i8, i4
     * 2. 	Document 13	i6, i8, i2
     * 3. 	Document 23	i10, i8
     * 4. 	Document 31	i8
     * 5. 	Document 33	i8, i7, i1
     * 6. 	Document 47	i8, i10
     * 7. 	Document 56	i8
     * 8. 	Document 58	i8, i2
     * 9. 	Document 69	i9, i8, i2
     * 10. 	Document 76	i8, i10
     */
    static StandardAnalyzer ANALYZER = new StandardAnalyzer();
    static Directory INDEX = new ByteBuffersDirectory();
    static IndexWriterConfig CONFIG = new IndexWriterConfig();
    static IndexWriter WRITER;
    static {
        try {
            WRITER = new IndexWriter(INDEX, CONFIG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static int SEARCH_COUNTER = 0;
    static String[] IAB_CATEGORIES = {"i1", "i2", "i3", "i4", "i5", "i6", "i7", "i8", "i9", "i10"};
    static Random RAND = new Random();
    static int MAX_CATEGORIES = 3;
    static int DOCUMENT_COUNTER = 0;
    static int DOCUMENTS_TO_ADD = 100000;

    static void addDoc(IndexWriter w) throws IOException {
        Document doc = new Document();

        doc.add(new TextField("title", "Document " + DOCUMENT_COUNTER, Field.Store.YES));
        DOCUMENT_COUNTER++;

        // Add a few IAB categories
        int categoriesToAdd = RAND.nextInt(MAX_CATEGORIES + 1);
        List<String> shuffledCategories = Arrays.asList(IAB_CATEGORIES);
        Collections.shuffle(shuffledCategories);
        for (int i = 0; i < categoriesToAdd; i++) {
            String category = IAB_CATEGORIES[i];
            doc.add(new StringField("category", category, Field.Store.YES));
        }

        w.addDocument(doc);
    }

    void setupIndex() throws IOException {
        for (int i =0; i < DOCUMENTS_TO_ADD; i++) {
            addDoc(WRITER);
        }
        WRITER.close();
    }

    Query makeQuery() throws ParseException {
        /*
         * Generate a query consisting of some IAB categories
         */
        int categoriesToAdd = RAND.nextInt(MAX_CATEGORIES + 1);
        String[] queries = new String[categoriesToAdd];
        List<String> shuffledCategories = Arrays.asList(IAB_CATEGORIES);
        Collections.shuffle(shuffledCategories);
        System.arraycopy(IAB_CATEGORIES, 0, queries, 0, categoriesToAdd);
        String joinedQuery = String.join(", ", queries);
        System.out.println("query=" + joinedQuery);
        return new QueryParser("category", ANALYZER).parse(queries[0]);
    }

    IndexSearcher openSearcher() throws IOException {
        IndexReader reader = DirectoryReader.open(INDEX);
        return new IndexSearcher(reader);
    }

    ScoreDoc[] search(IndexSearcher searcher, Query q) throws IOException {
        int hitsPerPage = 10;

        long start = System.nanoTime();
        TopDocs docs = searcher.search(q, hitsPerPage);
        long end = System.nanoTime();
        long durationMs = (end - start) / 1000;
        System.out.println("match duration " + SEARCH_COUNTER + " is " + durationMs + " microseconds");
        SEARCH_COUNTER++;

        return docs.scoreDocs;
    }

    void display(ScoreDoc[] hits, IndexSearcher searcher) throws IOException {
        System.out.println("Found " + hits.length + " hits.");
        for (int i=0; i < hits.length; i++) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            String categories = String.join(", ", d.getValues("category"));
            System.out.println((i+1) + ". " + "\t" + d.get("title") + "\t" + categories);
        }
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("There are " + DOCUMENTS_TO_ADD + " documents in the index.\n");
        Demo3 demo2 = new Demo3();
        demo2.setupIndex();
        Query q = demo2.makeQuery();
        IndexSearcher searcher = demo2.openSearcher();
        ScoreDoc[] hits = demo2.search(searcher, q);
        demo2.display(hits, searcher);
        ScoreDoc[] hits2 = demo2.search(searcher, q);
        demo2.display(hits2, searcher);
        ScoreDoc[] hits3 = demo2.search(searcher, q);
        demo2.display(hits3, searcher);
    }
}

package at.ac.univie.mminf.qskos4j.issues;

import at.ac.univie.mminf.qskos4j.QSkos;
import at.ac.univie.mminf.qskos4j.util.vocab.VocabRepository;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.OpenRDFException;

import java.io.IOException;

public class InvalidCharactersTest {
    private QSkos qSkosInvalidCharacters;

    @Before
    public void setUp() throws OpenRDFException, IOException {
        qSkosInvalidCharacters = new QSkos(VocabRepository.setUpFromTestResource("invalidCharacters.rdf"));
    }

    @Test
    public void testAllIssues() throws OpenRDFException {
        // all issues must run without exception
        try {
            for (Issue issue : qSkosInvalidCharacters.getAllIssues()) {
                issue.getReport();
            }
        }
        catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
}

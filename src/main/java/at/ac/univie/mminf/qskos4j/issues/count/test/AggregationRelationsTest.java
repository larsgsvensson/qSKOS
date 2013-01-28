package at.ac.univie.mminf.qskos4j.issues.count.test;

import at.ac.univie.mminf.qskos4j.issues.count.AggregationRelations;
import at.ac.univie.mminf.qskos4j.issues.count.SemanticRelations;
import at.ac.univie.mminf.qskos4j.util.test.IssueTestCase;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.OpenRDFException;

import java.io.IOException;

/**
 * Created by christian
 * Date: 26.01.13
 * Time: 14:47
 */
public class AggregationRelationsTest extends IssueTestCase {

    private AggregationRelations aggregationRelations;

    @Before
    public void setUp() throws OpenRDFException, IOException {
        aggregationRelations = (AggregationRelations) setUpIssue("aggregations.rdf", new SemanticRelations());
    }


    @Test
    public void testAggregationRelationsCount() throws OpenRDFException
    {
        Assert.assertEquals(7, aggregationRelations.getResult().getData().longValue());
    }

}
package at.ac.univie.mminf.qskos4j.issues.conceptscheme;

import at.ac.univie.mminf.qskos4j.issues.Issue;
import at.ac.univie.mminf.qskos4j.issues.concepts.AuthoritativeConcepts;
import at.ac.univie.mminf.qskos4j.result.CollectionResult;
import at.ac.univie.mminf.qskos4j.util.vocab.SkosOntology;
import at.ac.univie.mminf.qskos4j.util.vocab.SparqlPrefix;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.URIImpl;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.ArrayList;
import java.util.Collection;

public class MappingRelationsMisuse extends Issue<CollectionResult<Statement>> {

    private AuthoritativeConcepts authoritativeConcepts;

    public MappingRelationsMisuse(AuthoritativeConcepts authoritativeConcepts) {
        super(authoritativeConcepts,
            "mri",
            "Mapping Relations Misuse",
            "Finds concepts within the same concept scheme that are related by a mapping relation",
            IssueType.ANALYTICAL,
            new URIImpl("https://github.com/cmader/qSKOS/wiki/Quality-Issues#mapping-relations-misuse"));
        this.authoritativeConcepts = authoritativeConcepts;
    }

    @Override
    protected CollectionResult<Statement> invoke() throws RDF4JException {
        Collection<Statement> problematicRelations = new ArrayList<Statement>();

        RepositoryResult<Statement> result = repCon.getStatements(
                null,
                SkosOntology.getInstance().getUri("mappingRelation"),
                null,
                true);
        while (result.hasNext()) {
            Statement st = result.next();
            Resource concept = st.getSubject();
            Resource otherConcept = (Resource) st.getObject();

            if (areAuthoritativeConcepts(concept, otherConcept) &&
               (inSameConceptScheme(concept, otherConcept) || inNoConceptScheme(concept, otherConcept)))
            {
                problematicRelations.add(st);
            }
        }

        return new CollectionResult<Statement>(problematicRelations);
    }

    private boolean areAuthoritativeConcepts(Resource... concepts) throws RDF4JException {
        for (Resource concept : concepts) {
            boolean isAuthoritativeConcept = false;
            for (Resource authoritativeConcept : authoritativeConcepts.getResult().getData()) {
                if (concept.equals(authoritativeConcept)) isAuthoritativeConcept = true;
            }
            if (!isAuthoritativeConcept) return false;
        }

        return true;
    }

    private boolean inSameConceptScheme(Resource concept, Resource otherConcept) throws RDF4JException {
        return repCon.prepareBooleanQuery(QueryLanguage.SPARQL, createInSchemeQuery(concept, otherConcept)).evaluate();
    }

    private boolean inNoConceptScheme(Resource concept, Resource otherConcept) throws RDF4JException {
        boolean conceptInScheme = repCon.prepareBooleanQuery(QueryLanguage.SPARQL, createInSchemeQuery(concept)).evaluate();
        boolean otherConceptInScheme = repCon.prepareBooleanQuery(QueryLanguage.SPARQL, createInSchemeQuery(otherConcept)).evaluate();

        return !conceptInScheme || !otherConceptInScheme;
    }

    private String createInSchemeQuery(Resource... concepts) {
        String query = SparqlPrefix.SKOS + "ASK {";

        for (Resource concept : concepts) {
            query += "<" +concept.stringValue()+ "> skos:inScheme ?conceptScheme .";
        }

        return query + "}";
    }

}

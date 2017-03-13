package org.monarchinitiative.owlsim.services.modules;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.commons.validator.routines.UrlValidator;
import org.jboss.logging.Logger;
import org.monarchinitiative.owlsim.compute.classmatch.ClassMatcher;
import org.monarchinitiative.owlsim.compute.enrich.impl.HypergeometricEnrichmentEngine;
import org.monarchinitiative.owlsim.compute.matcher.impl.BayesianNetworkProfileMatcher;
import org.monarchinitiative.owlsim.compute.mica.MostInformativeCommonAncestorCalculator;
import org.monarchinitiative.owlsim.compute.mica.impl.MostInformativeCommonAncestorCalculatorImpl;
import org.monarchinitiative.owlsim.kb.BMKnowledgeBase;
import org.monarchinitiative.owlsim.kb.impl.BMKnowledgeBaseOWLAPIImpl;
import org.monarchinitiative.owlsim.services.modules.bindings.IndicatesDataTsvs;
import org.monarchinitiative.owlsim.services.modules.bindings.IndicatesOwlDataOntologies;
import org.monarchinitiative.owlsim.services.modules.bindings.IndicatesOwlOntologies;
import org.prefixcommons.CurieUtil;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * TODO: Uncomment the OwlKnowledgeBase code and remove unecessary code once happy.
 */
public class KnowledgeBaseModule extends AbstractModule {

	Logger logger = Logger.getLogger(KnowledgeBaseModule.class);

	private final ImmutableCollection<String> ontologyUris;
	private final ImmutableCollection<String> ontologyDataUris;
	private final ImmutableCollection<String> dataTsvs;
	private final ImmutableMap<String, String> curies;
	private final UrlValidator urlValdiator = UrlValidator.getInstance();

//	private final BMKnowledgeBase bmKnowledgeBase;

	public KnowledgeBaseModule(Collection<String> ontologyUris, Collection<String> ontologyDataUris, Set<String> dataTsvs, Map<String, String> curies) {
		this.ontologyUris = new ImmutableSet.Builder<String>().addAll(ontologyUris).build();
		this.ontologyDataUris = new ImmutableSet.Builder<String>().addAll(ontologyDataUris).build();
		this.dataTsvs = new ImmutableSet.Builder<String>().addAll(dataTsvs).build();
		this.curies = new ImmutableMap.Builder<String, String>().putAll(curies).build();

		logger.info("Loading ontologyUris:");
		ontologyUris.forEach(logger::info);
		logger.info("Loading ontologyDataUris:");
		ontologyDataUris.forEach(logger::info);
		logger.info("Loading dataTsvs:");
		dataTsvs.forEach(logger::info);
		logger.info("Loading curies:");
		curies.entrySet().forEach(logger::info);

//		//The OwlKnowledgeBase.Loader uses the ELKReasonerFactory and Concurrency.CONCURRENT as defaults.
//		this.bmKnowledgeBase = OwlKnowledgeBase.loader()
//				.loadOntologies(ontologyUris)
//				.loadDataFromOntologies(ontologyDataUris)
//				.loadDataFromTsv(dataTsvs)
//				.loadCuries(curies)
//				.createKnowledgeBase();
//
//		logger.info("Created BMKnowledgebase");
	}

	@Override
	protected void configure() {
		bind(BMKnowledgeBase.class).to(BMKnowledgeBaseOWLAPIImpl.class).in(Singleton.class);
		bind(OWLReasonerFactory.class).to(ElkReasonerFactory.class);
		bind(CurieUtil.class).toInstance(new CurieUtil(curies));

		// bind(OWLOntologyManager.class).to(OWLOntologyManagerImpl.class);
		// bind(ReadWriteLock.class).to(NoOpReadWriteLock.class);
		// bind(OWLDataFactory.class).to(OWLDataFactoryImpl.class);
		// bind(OWLOntologyManager.class).toInstance(OWLManager.createOWLOntologyManager());
	}

//	@Provides
//	BMKnowledgeBase provideBMKnowledgeBaseOWLAPIImpl() {
//		return bmKnowledgeBase;
//	}

	@Provides
	BMKnowledgeBaseOWLAPIImpl provideBMKnowledgeBaseOWLAPIImpl(@IndicatesOwlOntologies OWLOntology owlOntology,
			@IndicatesOwlDataOntologies OWLOntology owlDataOntology, OWLReasonerFactory rf, CurieUtil curieUtil) {
		return new BMKnowledgeBaseOWLAPIImpl(owlOntology,owlDataOntology, rf, curieUtil);
	}

	OWLOntology loadOntology(OWLOntologyManager manager, String uri) throws OWLOntologyCreationException {
		if (urlValdiator.isValid(uri)) {
			return manager.loadOntology(IRI.create(uri));
		} else {
			File file = new File(uri);
			return manager.loadOntologyFromOntologyDocument(file);
		}
	}

	OWLOntology mergeOntologies(OWLOntologyManager manager, Collection<String> uris)
			throws OWLOntologyCreationException, FileNotFoundException, IOException {
		OWLOntology ontology = manager.createOntology();
		for (String uri : uris) {
			OWLOntology loadedOntology;
			if (uri.endsWith(".gz")) {
				GZIPInputStream gis = new GZIPInputStream(new FileInputStream(uri));
				BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
				loadedOntology = manager.loadOntologyFromOntologyDocument(gis);
			} else {
				loadedOntology = loadOntology(manager, uri);
			}
			manager.addAxioms(ontology, loadedOntology.getAxioms());
		}
		return ontology;
	}

	@Provides
	@IndicatesOwlOntologies
	@Singleton
	OWLOntology getOwlOntologies(OWLOntologyManager manager)
			throws OWLOntologyCreationException, FileNotFoundException, IOException {
		return mergeOntologies(manager, ontologyUris);
	}

	@Provides
	@IndicatesOwlDataOntologies
	@Singleton
	OWLOntology getOwlDataOntologies(OWLOntologyManager manager)
			throws OWLOntologyCreationException, FileNotFoundException, IOException {
		return mergeOntologies(manager, ontologyDataUris);
	}

	@Provides
	@IndicatesDataTsvs
	@Singleton
	OWLOntology getDataTsvs(OWLOntologyManager manager)
			throws OWLOntologyCreationException, FileNotFoundException, IOException {
		return mergeOntologies(manager, dataTsvs);
	}
	
	@Provides
	MostInformativeCommonAncestorCalculator getMostInformativeCommonAncestorCalculator(BMKnowledgeBase knowledgeBase) {
        return new MostInformativeCommonAncestorCalculatorImpl(knowledgeBase);
	}
	
	@Provides
	HypergeometricEnrichmentEngine getHypergeometricEnrichmentEngine(BMKnowledgeBase knowledgeBase) {
	    return new HypergeometricEnrichmentEngine(knowledgeBase);
	}
	
	@Provides
	BayesianNetworkProfileMatcher getBayesianNetworkProfileMatcher(BMKnowledgeBase knowledgeBase) {
	    return BayesianNetworkProfileMatcher.create(knowledgeBase);
	}
	
	@Provides
	ClassMatcher getClassMatcher(BMKnowledgeBase knowledgeBase) {
	    return new ClassMatcher(knowledgeBase);
	}

}

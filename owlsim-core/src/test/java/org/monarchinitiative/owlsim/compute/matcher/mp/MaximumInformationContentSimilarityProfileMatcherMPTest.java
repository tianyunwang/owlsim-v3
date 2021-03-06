package org.monarchinitiative.owlsim.compute.matcher.mp;

import org.monarchinitiative.owlsim.compute.matcher.ProfileMatcher;
import org.monarchinitiative.owlsim.compute.matcher.impl.MaximumInformationContentSimilarityProfileMatcher;
import org.monarchinitiative.owlsim.kb.BMKnowledgeBase;

public class MaximumInformationContentSimilarityProfileMatcherMPTest extends AbstractProfileMatcherMPTest {

	protected ProfileMatcher createProfileMatcher(BMKnowledgeBase kb) {
		return MaximumInformationContentSimilarityProfileMatcher.create(kb);
	}

	@Override
	public void testSgDiseaseExact() throws Exception {
		testSgDiseaseExact(DISEASE.sg, null);

	}

	@Override
	public void testSgDiseaseLeaveOneOut() throws Exception {
		testSgDiseaseLeaveOneOut(DISEASE.sg, null);
	}


	@Override
	public void testMultiPhenotypeDisease() throws Exception {
		// ep, pd and foo all have equal ranking by max IC
		testMultiPhenotypeDisease(DISEASE.sg, null); // based on rarity of phenotype
	}


	@Override
	public void testEpDiseaseFuzzy() throws Exception {
		testEpDiseaseFuzzy(null, null);
	}


	@Override
	public void testNervousSystemDisease() throws Exception {
		testNervousSystemDisease(DISEASE.ep, null);
	}
	@Override
	public void testPdDisease() throws Exception {
		//testNervousSystemDisease(DISEASE.pd, 100);
		
	}
}

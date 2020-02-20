package refactoringml;

import org.junit.Assert;
import org.junit.Test;
import refactoringml.db.CommitMetaData;

import java.util.Random;

public class ProcessMetricTrackerTest {

	//TODO: write a test to test the ProcessMetricTracker reset function
	//TODO: write a test to test the ProcessMetricTracker isStable function
	//TODO: Write tests for complex StableCommit sequences
	@Test
	public void authorOwnership() {
		//TODO: What commit message should I use here?
		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", new CommitMetaData());

		for(int i = 0; i < 90; i++) {
			pm.reportCommit("commit","Mauricio", 10, 20);
		}

		for(int i = 0; i < 6; i++) {
			pm.reportCommit("commit","Diogo", 10, 20);
		}

		for(int i = 0; i < 4; i++) {
			pm.reportCommit("commit","Rafael", 10, 20);
		}

		Assert.assertEquals(3, pm.getCurrentProcessMetrics().qtyOfAuthors(), 0.0001);
		Assert.assertEquals(100, pm.getCurrentProcessMetrics().qtyOfCommits, 0.0001);
		Assert.assertEquals(0.90, pm.getCurrentProcessMetrics().authorOwnership(), 0.0001);
		Assert.assertEquals(2, pm.getCurrentProcessMetrics().qtyMajorAuthors());
		Assert.assertEquals(1, pm.getCurrentProcessMetrics().qtyMinorAuthors());
		Assert.assertEquals(0, pm.getCurrentProcessMetrics().bugFixCount);
	}

	@Test
	public void countBugFixes() {
		int qtyKeywords = ProcessMetricTracker.bugKeywords.length;
		Random rnd = new Random();

		ProcessMetricTracker pm = new ProcessMetricTracker("a.Java", new CommitMetaData());

		pm.reportCommit( "bug fix here","Rafael", 10, 20);

		int qty = 1;
		for(int i = 0; i < 500; i++) {
			String keywordHere = "";
			if(rnd.nextBoolean()) {
				keywordHere = ProcessMetricTracker.bugKeywords[rnd.nextInt(qtyKeywords - 1)];
				qty++;
			}

			pm.reportCommit("bla bla " + (keywordHere) + "ble ble","Rafael", 10, 20);
		}

		Assert.assertEquals(qty, pm.getCurrentProcessMetrics().bugFixCount);
	}
}

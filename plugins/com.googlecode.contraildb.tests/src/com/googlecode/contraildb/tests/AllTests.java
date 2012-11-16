package com.googlecode.contraildb.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {
    public static Test suite() {
        TestSuite ret = new AllTests();
        ret.addTestSuite(ReproduceVerifyError.class);
        ret.addTestSuite(NumberDecoderTest.class);
        ret.addTestSuite(KilimConcurrencyTests.class);
        ret.addTestSuite(ContrailTasksTests.class);
        ret.addTestSuite(RamStorageProviderTests.class);
        ret.addTestSuite(FileStorageProviderTests.class);
        ret.addTestSuite(ContrailBTreeTests.class);
        ret.addTestSuite(BasicContrailTests.class);
        ret.addTestSuite(ContrailStorageTests.class);
        return ret;
    }
}

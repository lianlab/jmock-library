/* Copyright (c) 2000-2003, jMock.org. See LICENSE.txt */
package test.jmock.dynamic;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import test.jmock.dynamic.testsupport.MockInvocationDispatcher;
import test.jmock.dynamic.testsupport.MockInvokable;
import test.jmock.dynamic.testsupport.MockStub;

import org.jmock.dynamic.CoreMock;
import org.jmock.dynamic.DynamicUtil;
import org.jmock.dynamic.Invocation;
import org.jmock.dynamic.LIFOInvocationDispatcher;
import org.jmock.expectation.AssertMo;
 

public class CoreMockTest extends TestCase {
    private static final String MOCK_NAME = "Test coreMock";

    private DummyInterface proxy;
    private CoreMock coreMock;
    private MockInvocationDispatcher mockDispatcher = new MockInvocationDispatcher();
    private MockInvokable mockInvokable = new MockInvokable();
    
    public void setUp() {
        coreMock = new CoreMock(DummyInterface.class, MOCK_NAME, mockDispatcher);

        try {
            proxy = (DummyInterface) coreMock.proxy();
        } catch (ClassCastException ex) {
            fail("proxy is not of expected interface type");
        }
    }
    
    public void testReportsMockedType() {
        assertSame( "mocked type", 
                    DummyInterface.class, coreMock.getMockedType() );
    }
    
    public void testMockAnnotatesAssertionFailedError()
            throws Throwable {
        final String originalMessage = "original message";

        Object arg = new AssertionFailedError(originalMessage);
        mockDispatcher.dispatchResult = arg;

        try {
            proxy.noArgVoidMethod();
        } catch (AssertionFailedError err) {
            AssertMo.assertIncludes("should contain original message", originalMessage, err.getMessage());
            AssertMo.assertIncludes("should contain coreMock name", MOCK_NAME, err.getMessage());
        }
    }

    public void testProxyReturnsConfiguredResult() throws Throwable {
        final String RESULT = "configured result";

        mockDispatcher.dispatchResult = RESULT;

        assertSame("result is returned by coreMock", RESULT, proxy.oneArgMethod("arg"));
    }

    public void testExceptionsPropagatedThroughProxy() throws Throwable {
        final Throwable throwable = new DummyThrowable();

        mockDispatcher.dispatchThrowable = throwable;

        try {
            proxy.noArgVoidMethod();
        } catch (Throwable ex) {
            assertSame("exception is caught by coreMock", throwable, ex);
            return;
        }
        fail("Should have thrown exception");
    }

    public void testMockVerifies() throws Exception {
        mockDispatcher.verifyCalls.setExpected(1);

        coreMock.verify();

        // Can't use Verifier as we are verifying "verify"
        mockDispatcher.verifyExpectations();
    }

    public void testTestsEqualityForProxy() throws Exception {
        coreMock = new CoreMock( DummyInterface.class, "coreMock",
                                 new LIFOInvocationDispatcher());
        proxy = (DummyInterface)coreMock.proxy();
        
        assertTrue( "should be equal", proxy.equals(proxy));
        assertFalse( "should not be equal", proxy.equals(new Object()) );
        assertFalse( "shuold not be equal to null", proxy.equals(null) );
    }

    public void testCanOverrideEqualsForProxyBySettingAStub() throws Exception {
        mockDispatcher.dispatchResult = new Boolean(false);
        
        mockDispatcher.dispatchInvocation.setExpected(
            new Invocation( proxy, DummyInterface.class, 
                            "equals", new Class[]{Object.class}, boolean.class, 
                            new Object[]{"not a proxy"} ));
        
        assertFalse( "Passes invocation of equals to dispatcher", 
                     proxy.equals("not a proxy") );
        
        mockDispatcher.verifyExpectations();
    }
    
    public void testCalculatesHashCodeForProxy() throws Exception {
        coreMock = new CoreMock( DummyInterface.class, "coreMock",
                                 new LIFOInvocationDispatcher());
        
        proxy = (DummyInterface)coreMock.proxy();
        
        assertEquals( "same hash code", proxy.hashCode(), proxy.hashCode() );
    }
    
    public void testCanOverrideHashCodeForProxyBySettingAStub() throws Exception {
        int expectedHashCode = 1;
        
        mockDispatcher.dispatchResult = new Integer(expectedHashCode);
        mockDispatcher.dispatchInvocation.setExpected(
            new Invocation( proxy, DummyInterface.class, 
                            "hashCode", new Class[0], int.class, 
                            new Object[0] ));
        
        assertEquals( "proxy hashCode", expectedHashCode, proxy.hashCode() );
        
        mockDispatcher.verifyExpectations();
    }
    
    public void testGeneratesMockNameFromInterfaceNameIfNoNameSpecified() throws Exception {
        assertEquals("mockString", CoreMock.mockNameFromClass(String.class));
    }

    public void testReturnsNameFromToString() {
        AssertMo.assertIncludes( "result of toString() should include name", 
        						 MOCK_NAME, coreMock.toString());
    }

    public void testProxyToString() throws Exception {
        assertEquals( "Should get a coreMock name without touching the underlying coreMock", 
    				  MOCK_NAME, DynamicUtil.toReadableString(proxy));
        coreMock.verify();  // should not fail on a proxyToString call
    }

    public void testAddsInvokablesToDispatcher() {
        mockDispatcher.addInvokable.setExpected(mockInvokable);

        coreMock.add(mockInvokable);

        mockDispatcher.verifyExpectations();
    }
    
    public void testExposesDefaultStubOfDispatcher() {
        MockStub dummyStub = new MockStub("dummyStub");
        
    	mockDispatcher.setDefaultStub.setExpected( dummyStub );
        
        coreMock.setDefaultStub( dummyStub );
        
        mockDispatcher.verifyExpectations();
    }

    public void testResetsDispatcher() {
        mockDispatcher.clearCalls.setExpected(1);

        coreMock.reset();

        mockDispatcher.verifyExpectations();
    }

    public void testVerifyFailuresIncludeMockName() {
        mockDispatcher.verifyFailure = new AssertionFailedError("verify failure");

        mockDispatcher.verifyCalls.setExpected(1);

        try {
            coreMock.verify();
        } catch (AssertionFailedError expected) {
            AssertMo.assertIncludes("Should include mock name", MOCK_NAME, expected.getMessage());
            mockDispatcher.verifyExpectations();
            return;
        }
        fail("Should have thrown exception");
    }
}
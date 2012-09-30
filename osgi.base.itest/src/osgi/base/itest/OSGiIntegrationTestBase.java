package osgi.base.itest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Base class for OSGi integration tests containing convenience methods for service handling.
 *
 * @author Christian Vogel
 *
 * @version 1.0.0 2012-09-16
 * @since 1.0.0
 */
public abstract class OSGiIntegrationTestBase extends TestCase {
	
	private static int testCount = 0;
	private static int totalTestCount = -1;
	
	/**
     * If we have to wait for a service, wait this amount of seconds.
     */
    private static final int SERVICE_TIMEOUT = 5;
    private BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
    
    /**
     * Convenience method to execute logic before a whole test case begins. 
     * 
     * @throws Exception thrown if something went wrong
     */
    protected void beforeTestCase() throws Exception {}
    
    /**
     * Convenience method to execute logic before each test. Initialize resources could be one example. 
     * 
     * @throws Exception thrown if something went wrong
     */
    protected void before() throws Exception {}
    
    /**
     * Convenience method to execute logic after a whole test case ends. 
     * 
     * @throws Exception thrown if something went wrong
     */
    protected void afterTestCase() throws Exception {}
    
    /**
     * Convenience method to execute logic after each test. Cleanup resources could be one example. 
     * 
     * @throws Exception thrown if something went wrong
     */
    protected void after() throws Exception {}
    
    /**
     * {@inheritDoc} 
     * <p>
     * Executes not only the bare test, but a before test and after test configuration as well.
     */
    @Override
    protected void runTest() throws Throwable {
    	if (totalTestCount == -1) {
			totalTestCount = countTotalTests();
		}
    	
    	if (testCount == 0) {
			beforeTestCase();
		}
    	
    	testCount++;
    	
    	before();
    	super.runTest();
    	after();
    	
    	if (testCount == totalTestCount) {
			totalTestCount = -1;
			testCount = 0;
			afterTestCase();
    	}
    }
    
    /**
     * Count the total number of tests containing in the derived class and all other subclasses.
     * 
     * @return number of test methods
     */
    private int countTotalTests() {
    	int count = 0;
    	Class<?> superClass = getClass();
    	List<String> names = new ArrayList<String>();
    	while (Test.class.isAssignableFrom(superClass)) {
    		Method[] methods = superClass.getDeclaredMethods();
    		for (Method method : methods) {
    			String name = method.getName();
    			
    			if (names.contains(name)) {
    				continue;
    			}
    			
    			names.add(name);
    			
    			if (isTestMethod(method)) {
    				count++;
    			}
    		}
    		
    		superClass = superClass.getSuperclass();
    	}
    	return count;
    }
    
    /**
     * Determines whether a given method is a test method or not.
     * 
     * @param m method to be checked
     * @return {@code true}, if the method is a test method, otherwise {@code false}
     */
    private boolean isTestMethod(Method m) {
    	String name = m.getName();
    	Class<?>[] parameters = m.getParameterTypes();
    	Class<?> returnType = m.getReturnType();
    	return parameters.length == 0 && name.startsWith("test") && returnType.equals(Void.TYPE);
    }
    
    /**
     * Convenience method to return an OSGi service.
     * 
     * @param serviceClass the service class to return.
     * @return a service instance, can be <code>null</code>.
     */
    protected <T> T getService(Class<T> serviceClass) {
        try {
            return getService(serviceClass, null);
        }
        catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convenience method to registers a new service to the OSGi environment.
     * 
     * @param serviceClass the service interface for the registration
     * @param impl implementation of the service interface to be registered
     * @param props configuration for the implementation
     */
    protected <T> void registerService(Class<T> serviceClass, T impl, Dictionary<String,?> props) {
    	bundleContext.registerService(serviceClass, impl, props);
    }
    
    /**
     * Write configuration for a single service.
     */
    protected void configure(String pid, Properties configuration) throws IOException {
        Configuration config = getConfiguration(pid);
        config.update(configuration);
    }
    
    /**
     * Gets an existing configuration or creates a new one, in case it does not exist.
     * 
     * @param pid the PID of the configuration to return.
     * @return a {@link Configuration} instance, never <code>null</code>.
     * @throws IOException if access to the persistent storage failed.
     */
    protected Configuration getConfiguration(String pid) throws IOException {
        ConfigurationAdmin admin = getService(ConfigurationAdmin.class);
        return admin.getConfiguration(pid, null);
    }
    
    /**
     * Convenience method to return an OSGi service.
     * 
     * @param serviceClass the service class to return;
     * @param filterString the (optional) filter string, can be <code>null</code>.
     * @return a service instance, can be <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getService(Class<T> serviceClass, String filterString) throws Exception {
        T serviceInstance = null;

        ServiceTracker serviceTracker;
        if (filterString == null) {
            serviceTracker = new ServiceTracker(bundleContext, serviceClass.getName(), null);
        }
        else {
            String classFilter = "(" + Constants.OBJECTCLASS + "=" + serviceClass.getName() + ")";
            filterString = "(&" + classFilter + filterString + ")";
            serviceTracker = new ServiceTracker(bundleContext, bundleContext.createFilter(filterString), null);
        }
        serviceTracker.open();
        try {
            serviceInstance = (T) serviceTracker.waitForService(SERVICE_TIMEOUT * 1000);

            if (serviceInstance == null) {
                fail(serviceClass + " service not found.");
            }
            else {
                return serviceInstance;
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            fail(serviceClass + " service not available: " + e.toString());
        }

        return serviceInstance;
    }

}

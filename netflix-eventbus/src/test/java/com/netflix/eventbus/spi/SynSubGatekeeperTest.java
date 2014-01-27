package com.netflix.eventbus.spi;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.eventbus.utils.EventBusUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

public class SynSubGatekeeperTest {

    public static final String ALLOW_ALL_METHODS_JSON = "{\"" + MySyncSub.class.getName() + "\" : []}";
    public static final String ALLOW_STRING_EVENT_JSON = "{\"" + MySyncSub.class.getName() + "\" : [\"" + String.class.getName() + "\"]}";
    public static final String ALLOW_MULTI_SUBS_JSON = "{\"" + MySyncSub.class.getName() + "\" : [\"" + String.class.getName() + "\"], \"" + MySyncSub2.class.getName() + "\" : [\"" + Double.class.getName() + "\"]}";

    @Before
    public void setUp() throws Exception {
        ConfigurationManager.getConfigInstance().clearProperty(SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS);
        ConfigurationManager.getConfigInstance().clearProperty(
                SyncSubscribersGatekeeper.SYNC_SUBSCRIBERS_WHITELIST_JSON);
        SyncSubscribersGatekeeper.initState();
    }

    @Test
    public void testEmptyWhitelist() throws Exception {
        ConcurrentCompositeConfiguration config = (ConcurrentCompositeConfiguration)ConfigurationManager.getConfigInstance();
        config.setOverrideProperty(SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, "true");
        config.setOverrideProperty(SyncSubscribersGatekeeper.SYNC_SUBSCRIBERS_WHITELIST_JSON, "");

        Assert.assertTrue("Empty white list does not allow all subs as sync.", checkConsumeAllowed(new MySyncSub(), String.class));
    }

    @Test
    public void testAllowAllSyncWithWhitelist() throws Exception {
        ConcurrentCompositeConfiguration config = (ConcurrentCompositeConfiguration)ConfigurationManager.getConfigInstance();
        config.setOverrideProperty(SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, "true");
        config.setOverrideProperty(SyncSubscribersGatekeeper.SYNC_SUBSCRIBERS_WHITELIST_JSON,
                ALLOW_ALL_METHODS_JSON);
        
        Assert.assertTrue("Allow all whitelist did not allow string event sync.", checkConsumeAllowed(new MySyncSub(), String.class));
        Assert.assertTrue("Allow all whitelist did not allow double event sync.", checkConsumeAllowed(new MySyncSub(), Double.class));
    }

    @Test
    public void testAllowSelectiveSyncWithWhitelist() throws Exception {
        ConcurrentCompositeConfiguration config = (ConcurrentCompositeConfiguration)ConfigurationManager.getConfigInstance();
        config.setOverrideProperty(SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, "true");
        config.setOverrideProperty(SyncSubscribersGatekeeper.SYNC_SUBSCRIBERS_WHITELIST_JSON,
                ALLOW_STRING_EVENT_JSON);
        
        Assert.assertTrue("Allow string whitelist did not allow string event sync.", checkConsumeAllowed(new MySyncSub(), String.class));
        Assert.assertFalse("Allow string whitelist allowed double event sync.", checkConsumeAllowed(new MySyncSub(), Double.class));
    }

    @Test
    public void testAllowMultiSubs() throws Exception {
        ConcurrentCompositeConfiguration config = (ConcurrentCompositeConfiguration)ConfigurationManager.getConfigInstance();
        config.setOverrideProperty(SyncSubscribersGatekeeper.ALLOW_SYNC_SUBSCRIBERS, "true");
        config.setOverrideProperty(SyncSubscribersGatekeeper.SYNC_SUBSCRIBERS_WHITELIST_JSON, ALLOW_MULTI_SUBS_JSON);

        Assert.assertTrue("Allow multi whitelist did not allow string event sync on sub1.", checkConsumeAllowed(new MySyncSub(), String.class));
        Assert.assertTrue("Allow multi whitelist did not allow double event sync on sub2.", checkConsumeAllowed(new MySyncSub2(), Double.class));

        Assert.assertFalse("Allow multi whitelist allowed double event sync on sub1.", checkConsumeAllowed(new MySyncSub(), Double.class));
        Assert.assertFalse("Allow multi whitelist allowed string event sync on sub2.", checkConsumeAllowed(new MySyncSub2(), String.class));
    }

    @SuppressWarnings("unchecked")
    private boolean checkConsumeAllowed(Object subInstance, Class eventClass)
            throws NoSuchMethodException, IllegalAccessException, InstantiationException {
        Class subClass = subInstance.getClass();
        Method consumeStr = subClass.getDeclaredMethod("consume", eventClass);
        SubscriberConfigProvider.SubscriberConfig subscriberConfig = EventBusUtils.getSubscriberConfig(consumeStr, subInstance);
        return SyncSubscribersGatekeeper.isSyncSubscriber(subscriberConfig, eventClass, subClass);
    }

    private static class MySyncSub {

        @Subscribe(syncIfAllowed = true)
        private void consume(String str) {

        }

        @Subscribe(syncIfAllowed = true)
        private void consume(Double str) {

        }
    }

    private static class MySyncSub2 {

        @Subscribe(syncIfAllowed = true)
        private void consume(String str) {

        }

        @Subscribe(syncIfAllowed = true)
        private void consume(Double str) {

        }
    }

}

package com.netflix.eventbus.spi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A gatekeeper to allow synchronous subscribers in {@link com.netflix.eventbus.impl.EventBusImpl}. This determines whether a particular
 * subscriber annotated with {@link com.netflix.eventbus.spi.Subscribe#syncIfAllowed()} as <code>true</code>  is
 * actually allowed to be synchronous.
 *
 * A subscriber is allowed, iff,
 * <ul>
    <li>The property {@link SyncSubscribersGatekeeper#ALLOW_SYNC_SUBSCRIBERS} is <code>true</code> </li>
    <li>If a property {@link SyncSubscribersGatekeeper#SYNC_SUBSCRIBERS_WHITELIST_JSON} exists, is non empty
 and contains this subscriber.</li>
 </ul>
 *
 * <h2>Whitelisting: </h2>
 *
 * One can cherry-pick subscribers which are allowed to be synchronous. This can be done by setting a property
 * {@link SyncSubscribersGatekeeper#SYNC_SUBSCRIBERS_WHITELIST_JSON} with a json of the following format:
 *
 <PRE>
    {
        "[Fully qualified class name of the subscriber class]": 0 or more fully qualified class name of the events,
    }
 </PRE>

 <b>Example: </b>

 <PRE>
 {
     "com.foo.bar.MyAllSubscriber": [], // Signifies all subscriber methods in this class will be sync, if configured so by setting {@link com.netflix.eventbus.spi.Subscribe#syncIfAllowed()} as <code>true</code>.
     "com.foo.bar.MySubscriber": ["com.foo.bar.EventOne", "com.foo.bar.EventTwo"], // Signifies only subscrber of events EventOne &amp; EventTwo in this class will be sync, if configured so by setting {@link com.netflix.eventbus.spi.Subscribe#syncIfAllowed()} as <code>true</code>.
     "com.foo.bar.MyAnotherSubscriber": ["com.foo.bar.Event3", "com.foo.bar.Event4"]
 }

 </PRE>
 *
 * @author Nitesh Kant
 */
public class SyncSubscribersGatekeeper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncSubscribersGatekeeper.class);

    public static final String ALLOW_SYNC_SUBSCRIBERS = "eventbus.allow.sync.subscribers";

    /**
     * Property to define a whitelist of subscribers which are allowed to be synchronous.
     *
     * See {@link SyncSubscribersGatekeeper} javadocs for details of the format of this property.
     */
    public static final String SYNC_SUBSCRIBERS_WHITELIST_JSON = "eventbus.sync.subscribers.whitelist.json";

    public static final SetMultimap<String,String> EMPTY_WHITELIST = Multimaps.forMap(Collections.<String, String>emptyMap());

    private static DynamicBooleanProperty allowSyncSubs;

    private static AtomicReference<SetMultimap<String, String>> syncSubsWhiteList;

    private static DynamicStringProperty syncSubsWhitelistJson;

    static {
        initState();
    }

    private static final TypeToken<SetMultimap<String, String>> whitelistTypeToken = new TypeToken<SetMultimap<String, String>>() {};

    public static final String ALLOW_ALL_EVENTS = "*";

    private static final Gson whiteListJsonParser =
            new GsonBuilder().registerTypeAdapter(whitelistTypeToken.getType(), new JsonDeserializer<SetMultimap<String, String>>() {
                @Override
                public SetMultimap<String, String> deserialize(JsonElement jsonElement, Type type,
                                                               JsonDeserializationContext jsonDeserializationContext)
                        throws JsonParseException {
                    final SetMultimap<String, String> toReturn =
                            Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(), new Supplier<Set<String>>() {
                                @Override
                                public Set<String> get() {
                                    return new HashSet<String>();
                                }
                            });
                    for (Map.Entry<String, JsonElement> entry : ((JsonObject) jsonElement).entrySet()) {
                        for (JsonElement element : (JsonArray) entry.getValue()) {
                            String value = element.getAsString();
                            if (null != value && !value.isEmpty()) {
                                toReturn.get(entry.getKey()).add(value);
                            }
                        }
                        if (!toReturn.containsKey(entry.getKey())) {
                            toReturn.put(entry.getKey(), ALLOW_ALL_EVENTS);
                        }
                    }

                    return toReturn;
                }
            }).create();

    /**
     * Deduce whether the subscriber favors synchronous event consumption. See {@link SyncSubscribersGatekeeper} javadoc
     * for details of when will this be allowed.
     *
     *
     * @param subscribe The configuration for the subscriber in question.
     * @param eventClass Class of the event for which this check is to be done.
     * @param subscriberClass Class of the subscriber for which this check is to be done.
     *
     * @return <code>true</code> if the subscriber should be provided events synchronously.
     */
    public static boolean isSyncSubscriber(SubscriberConfigProvider.SubscriberConfig subscribe, Class eventClass,
                                           Class subscriberClass) {
        if (subscribe.syncIfAllowed() && allowSyncSubs.get()) {
            SetMultimap<String, String> whiteList = syncSubsWhiteList.get();
            if (whiteList.isEmpty() || !whiteList.containsKey(subscriberClass.getName())) {
                return true;
            } else {
                Set<String> allowedEvents = whiteList.get(subscriberClass.getName());
                return allowedEvents.contains(ALLOW_ALL_EVENTS) || allowedEvents.contains(eventClass.getName());
            }
        }
        return false;
    }

    @VisibleForTesting
    static void initState() {
        syncSubsWhiteList = new AtomicReference<SetMultimap<String, String>>(EMPTY_WHITELIST);
        allowSyncSubs =
                DynamicPropertyFactory.getInstance().getBooleanProperty(ALLOW_SYNC_SUBSCRIBERS, true);
        syncSubsWhitelistJson =
                DynamicPropertyFactory.getInstance().getStringProperty(
                        SyncSubscribersGatekeeper.SYNC_SUBSCRIBERS_WHITELIST_JSON, "",
                        new Runnable() {
                            @Override
                            public void run() {
                                populateSubsWhiteList();
                            }
                        }
                );
    }

    private static void populateSubsWhiteList() {
        SetMultimap<String, String> originalVal = syncSubsWhiteList.get();

        String newWhiteListStr = syncSubsWhitelistJson.get();
        SetMultimap<String, String> newVal = EMPTY_WHITELIST;
        if (null != newWhiteListStr && !newWhiteListStr.isEmpty()) {
            try {
                SetMultimap<String, String> parsed = whiteListJsonParser.fromJson(newWhiteListStr, whitelistTypeToken.getType());
                if (null != parsed) {
                    newVal = parsed;
                }
            } catch (JsonParseException e) {
                LOGGER.error(String.format("Illegal value %s for property %s. The value should be a json that can be converted to a type %s. Ignoring this change.",
                        newWhiteListStr, SYNC_SUBSCRIBERS_WHITELIST_JSON, whitelistTypeToken.getType()),
                        e);
            }
        }

        if (!syncSubsWhiteList.compareAndSet(originalVal, newVal)) {
            LOGGER.debug("Sync subscribers whitelist concurrently modified, ignoring this change: " + newWhiteListStr);
        } else {
            LOGGER.info("Sync subscribers whitelist updated to: " + newWhiteListStr);
        }
    }
}

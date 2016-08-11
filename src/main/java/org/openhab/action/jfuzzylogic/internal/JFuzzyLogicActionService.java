/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.jfuzzylogic.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.core.scriptengine.action.ActionService;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class registers an OSGi service for the fuzzyLogic action.
 *
 * @author ansz
 * @since 1.0.0
 */
public class JFuzzyLogicActionService implements ActionService, ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(JFuzzyLogicActionService.class);

    /**
     * The BundleContext. This is only valid when the bundle is ACTIVE. It is set in the activate()
     * method and must not be accessed anymore once the deactivate() method was called or before activate()
     * was called.
     */
    private BundleContext bundleContext;

    /** RegEx to validate a config <code>'^(.*?)\\.(host|port)$'</code> */
    private static final Pattern EXTRACT_JFUZZYLOGIC_CONFIG_PATTERN = Pattern
            .compile("^(.*?)\\.(FclFilePath|InParamList|OutParam)$");

    protected static Map<String, FuzzyLogicConfig> fuzzyLogicConfigCache = new HashMap<String, FuzzyLogicConfig>();

    /**
     * Indicates whether this action is properly configured which means all
     * necessary configurations are set. This flag can be checked by the
     * action methods before executing code.
     */
    /* default */ static boolean isProperlyConfigured = false;

    public JFuzzyLogicActionService() {
    }

    /**
     * Called by the SCR to activate the component with its configuration read from CAS
     *
     * @param bundleContext BundleContext of the Bundle that defines this component
     * @param configuration Configuration properties for this component obtained from the ConfigAdmin service
     */
    public void activate(final BundleContext bundleContext, final Map<String, Object> configuration) {
        this.bundleContext = bundleContext;

        // the configuration is guaranteed not to be null, because the component definition has the
        // configuration-policy set to require. If set to 'optional' then the configuration may be null

        // read config parameters here ...

        isProperlyConfigured = true;
    }

    /**
     * Called by the SCR when the configuration of a binding has been changed through the ConfigAdmin service.
     *
     * @param configuration Updated configuration properties
     */
    public void modified(final Map<String, Object> configuration) {
        // update the internal configuration accordingly
    }

    /**
     * Called by the SCR to deactivate the component when either the configuration is removed or
     * mandatory references are no longer satisfied or the component has simply been stopped.
     *
     * @param reason Reason code for the deactivation:<br>
     *            <ul>
     *            <li>0 – Unspecified
     *            <li>1 – The component was disabled
     *            <li>2 – A reference became unsatisfied
     *            <li>3 – A configuration was changed
     *            <li>4 – A configuration was deleted
     *            <li>5 – The component was disposed
     *            <li>6 – The bundle was stopped
     *            </ul>
     */
    public void deactivate(final int reason) {
        this.bundleContext = null;
        // deallocate resources here that are no longer needed and
        // should be reset when activating this binding again
    }

    @Override
    public String getActionClassName() {
        return JFuzzyLogic.class.getCanonicalName();
    }

    @Override
    public Class<?> getActionClass() {
        return JFuzzyLogic.class;
    }

    @Override
    public void updated(Dictionary<String, ?> config) throws ConfigurationException {
        logger.debug("Starting to read FuzzyLogic configuration");
        if (config != null) {
            Enumeration<String> keys = config.keys();

            while (keys.hasMoreElements()) {
                String key = keys.nextElement();

                // the config-key enumeration contains additional keys that we
                // don't want to process here ...
                if ("service.pid".equals(key)) {
                    continue;
                }

                Matcher matcher = EXTRACT_JFUZZYLOGIC_CONFIG_PATTERN.matcher(key);

                if (!matcher.matches()) {
                    logger.debug("given config key '" + key
                            + "' does not follow the expected pattern '<FunctionBlockName>.<FclFilePath|InParamList|OutParam>'");
                    continue;
                }

                matcher.reset();
                matcher.find();

                String functionBlockName = matcher.group(1);

                FuzzyLogicConfig fuzzyLogicConfig = fuzzyLogicConfigCache.get(functionBlockName);

                if (fuzzyLogicConfig == null) {
                    fuzzyLogicConfig = new FuzzyLogicConfig(functionBlockName);
                    fuzzyLogicConfigCache.put(functionBlockName, fuzzyLogicConfig);
                }

                String configKey = matcher.group(2);
                String value = (String) config.get(key);

                if ("FclFilePath".equals(configKey)) {
                    fuzzyLogicConfig.fclFilePath = value;
                    logger.debug("functionBlockName=" + functionBlockName + ".fclFilePath=" + value);
                } else if ("InParamList".equals(configKey)) {
                    logger.debug("functionBlockName=" + functionBlockName + ".InParamList=" + value);
                    for (String inParam : value.split(",")) {
                        fuzzyLogicConfig.inParamNames.add(inParam.trim());
                        logger.debug("functionBlockName=" + functionBlockName + ".inParamName=" + inParam);
                    }
                } else if ("OutParam".equals(configKey)) {
                    fuzzyLogicConfig.outParamName = value;
                    logger.debug("functionBlockName=" + functionBlockName + ".outParamName=" + value);
                } else {
                    throw new ConfigurationException(configKey, "the given configKey '" + configKey + "' is unknown");
                }
            }
            isProperlyConfigured = true;
        }
    }

    /**
     * Internal data structure which carries the configuration details of one
     * fuzzy logic function block (there could be several)
     */
    class FuzzyLogicConfig {

        private String functionBlockName;
        private String fclFilePath;
        private ArrayList<String> inParamNames = new ArrayList<>(3);
        private String outParamName;

        public FuzzyLogicConfig(String functionBlockName) {
            this.functionBlockName = functionBlockName;
        }

        public String getFunctionBlockName() {
            return this.functionBlockName;
        }

        public String getFclFilePath() {
            return this.fclFilePath;
        }

        public ArrayList<String> getInPramNames() {
            return this.inParamNames;
        }

        public String getOutPramName() {
            return this.outParamName;
        }

        @Override
        public String toString() {
            return "FuzzyLogic [FunctionBlock=" + this.functionBlockName + "fcl filePath=" + this.fclFilePath
                    + " inParameters=" + this.inParamNames.toString() + ", outParemter=" + this.outParamName + "]";
        }

    }
}

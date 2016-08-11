package org.openhab.action.jfuzzylogic.internal;

import java.util.ArrayList;

import org.openhab.core.scriptengine.action.ActionDoc;
import org.openhab.core.scriptengine.action.ParamDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.rule.Variable;

public class JFuzzyLogic {

    private static final Logger logger = LoggerFactory.getLogger(JFuzzyLogic.class);

    @ActionDoc(text = "A method that calculate fuzzy logic function block using configuration and inParamValues", returns = "Calculation result as Double")
    public static Double doFuzzyLogic(
            @ParamDoc(name = "functionBlock", text = "function block name") String functionBlock,
            @ParamDoc(name = "inParamValues", text = "input parameters values") ArrayList<Double> inParamValues) {
        logger.debug("calcFuzzyLogic: Start");
        logger.debug(functionBlock + " " + inParamValues.toString());

        if (JFuzzyLogicActionService.isProperlyConfigured) {
            JFuzzyLogicActionService.FuzzyLogicConfig fuzzyLogicConfig = JFuzzyLogicActionService.fuzzyLogicConfigCache
                    .get(functionBlock);
            try {
                // Load FIS
                FIS fis = FIS.load(fuzzyLogicConfig.getFclFilePath(), true);

                ArrayList<String> inParamNames = fuzzyLogicConfig.getInPramNames();

                if (inParamNames.size() != inParamValues.size()) {
                    throw new IllegalArgumentException("Diferent size of input parameters and input values table :"
                            + functionBlock + " inParamNames=" + inParamNames.toString() + " inParamValues="
                            + inParamValues.toString());
                }

                for (int i = 0; i < inParamNames.size(); i++) {
                    logger.debug("inParamNames[" + i + "]=" + inParamNames.get(i));
                    logger.debug("inParamValues[" + i + "]=" + inParamValues.get(i));
                    fis.getFunctionBlock(functionBlock).setVariable(inParamNames.get(i), inParamValues.get(i));
                }

                // Evaluate FIS
                fis.getFunctionBlock(functionBlock).evaluate();

                String outParamName = fuzzyLogicConfig.getOutPramName();
                Variable variable = fis.getFunctionBlock(functionBlock).getVariable(outParamName);
                logger.debug("Fuzzy logic successfully evaluated for " + functionBlock);

                return variable.getDefuzzifier().defuzzify();

            } catch (Exception ex) {
                logger.error("Failed to evaluate Fuzzy logic = " + functionBlock, ex);
                return null;
            }
        } else {
            logger.error("doFuzzyLogic: action is not configured");
            return null;
        }

    }

}

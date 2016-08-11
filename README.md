This action extends openhab 1.x actions set by fuzzy logic features using the most complete fuzzy logic library in Java http://jfuzzylogic.sourceforge.net.

Only what you need to do is to configure fcl file, openhab.cfg and use doFuzzyLogic action in some rule ([action jar file] (https://github.com/ansz74/org.openhab.action.jfuzzylogic/blob/master/target/org.openhab.action.jfuzzylogic-1.9.0-SNAPSHOT.jar) must be first added to addon library).

Before you start using action in openhab please read jfuzzylogic library [manual] (http://jfuzzylogic.sourceforge.net/html/manual.html)

#Example of usage:
##openhab.cfg
```
############################### jFuzzyLogic action ##############################$
#
#jFuzzyLogic set #1 configuration
#jfuzzylogic:<function_block#1>.FclFilePath=/etc/openhab/configurations...
#jfuzzylogic:<function_block#1>.InParamList=inparam1,inparam2,inparam3
#jfuzzylogic:<function_block#1>.OutParam=outparam

#jFuzzyLogic set #2 configuration
#jfuzzylogic:<function_block#2>.FclFilePath=/etc/openhab/configurations...
#jfuzzylogic:<function_block#2>.InParamList=inparam1,inparam2,inparam3
#jfuzzylogic:<function_block#2>.OutParam=outparam

jfuzzylogic:VENTILATION.FclFilePath=/etc/openhab/configurations/ventilation.fcl
jfuzzylogic:VENTILATION.InParamList=air_quality, internal_temperature, external_temperature
jfuzzylogic:VENTILATION.OutParam=fun_level
```
* VENTILATION - FUNCTION_BLOCK defined in fcl file
* FclFilePath - path to fcl file
* InParamList - list of input parameters defined in fcl file (VAR_INPUT)
* OutParam - output parameter defined in fcl file (VAR_OUTPUT)

##fcl file:
Prepare fcl file according to this [documentation] (http://jfuzzylogic.sourceforge.net/html/manual.html#details)

jfuzzylogic library contains also very usefull diagnostic and evaluation [tools]  (http://jfuzzylogic.sourceforge.net/html/manual.html#commandline) which can be used to perform quick tests before final implementation in openhab

You can find ventilation.fcl file as an example [here] (https://github.com/ansz74/org.openhab.action.jfuzzylogic/blob/master/src/main/resources/ventilation.fcl)  

##rule:
- doFuzzyLogic(FUNCTION_BLOCK as String, in_params_values as ArrayList) returns OutParam as double value
- Keep in mind that in_params_values order must be the same as defined in InParamList in openhab.cfg

```
rule "VentilationFuzzyLogic"
when
	Item VentMode	changed
	or
	Time cron "0 0/10 * * * ?"
then
	logInfo("VentilationFuzzyLogic","Start")
	if (VentMode.state	instanceof StringType 	&&
		EnvCO2AVG.state instanceof DecimalType	&&
		gTemp.state 	instanceof DecimalType 	&&
		comfoairIndoorIncommingTemperature.state instanceof DecimalType) {
		if (VentMode.state == "AUTO") {
			val vInParams = newArrayList((EnvCO2AVG.state as DecimalType).doubleValue, 							//avg co2 level
										(gTemp.state as DecimalType).doubleValue,								//avg internal temperature
										(comfoairIndoorIncommingTemperature.state as DecimalType).doubleValue)	//incomming temperature
			var Number vFunLevel = doFuzzyLogic("VENTILATION",vInParams)						// do fuzzy logic magic jFuzzyLogic
			vFunLevel = Math::round(vFunLevel.floatValue)
			logInfo("VentilationFuzzyLogic","vFunLevel=" + vFunLevel.toString)
			if (comfoairFanLevel.state != vFunLevel) {
				sendCommand(comfoairFanLevel,vFunLevel.intValue)
			}

		}
	}
	logInfo("VentilationFuzzyLogic","Stop")
end
```

This action extends openhab 1.x actions by fuzzy logic features using the most complete fuzzy logic library in Java http://jfuzzylogic.sourceforge.net.

Before you start using action please read jfuzzylogic library manual http://jfuzzylogic.sourceforge.net/html/manual.html


What you need is to configure fcl file, openhab.cfg and use doFuzzyLogic action (jar file must be first added to addon library).

Information about fcl file can be found on http://jfuzzylogic.sourceforge.net/html/manual.html#details
Configuration of openhab.cfg:
############################### jFuzzyLogic action ##############################$
#
#jfuzzylogic:<function_block#1>.FclFilePath=/etc/openhab/configurations...
#jfuzzylogic:<function_block#1>.InParamList=inparam1,inparam2,inparam3
#jfuzzylogic:<function_block#1>.OutParam=outparam
# jFuzzyLogic set #1 configuration


Example of usage:
openhab.cfg
jfuzzylogic:VENTILATION.FclFilePath=/etc/openhab/configurations/ventilation.fcl
jfuzzylogic:VENTILATION.InParamList=air_quality, internal_temperature, external_temperature
jfuzzylogic:VENTILATION.OutParam=fun_level

rule:
rule "VentilationFuzzyLogic"
when
	Item VentMode					changed
	or
	Time cron "0 0/10 * * * ?"
then
	logInfo("VentilationFuzzyLogic","Start")
	if (VentMode.state	instanceof StringType) {
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

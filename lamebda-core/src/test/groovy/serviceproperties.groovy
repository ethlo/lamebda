/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 - 2019 Morten Haraldsen (ethlo)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import com.ethlo.lamebda.*
import java.util.Date;

class serviceproperties extends SimpleServerFunction {

    serviceproperties()
    {
        super("/serviceproperties/**");
    }

    // Configuration properties
    def appConfig = "/etc/%appname/application.properties"
    def appConfigDefaults = "/etc/%appname/application-properties.defaults"
    def allowedServices = "foo-"

    // WARNING: Do take care, command is run unchecked with permission of the service
    def runCommand(String command) {
        def sout = new StringBuffer(), serr = new StringBuffer()
        def commandLine = ['/bin/bash', '-c', "${command}" ]
        def proc = commandLine.execute()
        proc.consumeProcessOutput(sout, serr)
        proc.waitFor()
        Thread.sleep(200) // Sleep a bit to allow for flushing of result data
        // return "out:$sout err:$serr exitcode:${proc.exitValue()}" // For debugging
        return sout.toString().trim()
    }

    def isApplicationAllowed(appname) {
        return (appname.indexOf(allowedServices)==0)
    }

    def getApplicationPropertyFile(applicationName) {
        return appConfig.replace('%appname', applicationName) // Assemble app specific config path
    }

    def getApplicationPropertyDefaultsFile(applicationName) {
        return appConfigDefaults.replace('%appname', applicationName) // Assemble app specific config path
    }

    // Fetch the value of a key in an basic key-value property file
    def getPropertyValue(propertyFile, propertyKey) {
        return runCommand("cat $propertyFile | grep \"$propertyKey=.*\" | awk -F = '{print \$2 } '")
    }

    def getPropertyKeyValues(propertyFile, propertyKeyExpr) {
        return runCommand("cat $propertyFile | cut -d '#' -f1 | grep \"$propertyKeyExpr\" | awk -F = '{print \$1 , \$2 } '")
    }

    def setPropertyValue(propertyFile, propertyKey, propertyValue) {
        runCommand("grep -q \"$propertyKey=\" $propertyFile && sed -i 's/^[[:blank:]]*$propertyKey=.*/$propertyKey=$propertyValue/' $propertyFile || echo \"$propertyKey=$propertyValue\" >> $propertyFile")
    }

    def removeProperty(propertyFile, propertyKey) {
        runCommand("sed -i \"/$propertyKey/d\" $propertyFile")
    }

    def fileExists(filePath) {
        return (new File(filePath)).exists()
    }

    @Override
    void get(HttpRequest request, HttpResponse response) {
        def _result = runCommand("ls -d /etc/${allowedServices}* ") // List installed and allowed apps

        // Allow for retrival only of some configuration values
        def _appName = this.getClass().getSimpleName().toString()
        def _directAction = request.path().substring(request.path().indexOf(_appName)+(_appName.length()))
        def _directApp = _directAction.replaceAll(/^\//,'') // Trimming starting slashes if any

        // Check if any app specified, if not skip
        if(_directApp != "" && _directApp.indexOf('/')!=-1) {
            _directApp = _directApp.substring(0, _directApp.indexOf('/')).replaceAll(/$\//,'') // Trailing slashes if present

            // Check that application is allowed to be invoked at all
            if(isApplicationAllowed(_directApp)) {
                def _directAppFile = getApplicationPropertyFile(_directApp) // Assemble app specific config path
                def _directAppDefaultsFile = getApplicationPropertyDefaultsFile(_directApp) // Assemble app specific defaults path
                def _directAppPropertyExpr = _directAction.substring(_directAction.lastIndexOf('/')+1)

                // Default to echoing back current status of property manipulated
                _result = getPropertyKeyValues(_directAppFile, _directAppPropertyExpr).split('\n') // Return response as array listing
                def _defaults = getPropertyKeyValues(_directAppDefaultsFile, _directAppPropertyExpr).split('\n')

                // If empty result, set an empty message
                if(_result.length==1 && _result[0]=="") {
                    _result = 'No property set in application config'
                }

                response.json(HttpStatus.OK, [application: _directApp, action: 'READ', keys: _directAppPropertyExpr, values: _result, defaults: _defaults])
            }
        }

        response.json(HttpStatus.OK, [message:'Property service available:', server: request.remoteIpAddress(), result: _result, modified: new Date().toString()])
    }

    @Override
    void post(HttpRequest request, HttpResponse response) {
        def input = request.json()
        def _application = input.application
        def _action = input.action
        def _propertyKeys = input.keys
        def _propertyValue = input.value
        // Local outputs
        def _propertyFile = null
        def _propertyDefaultsFile = null
        def _values = null
        def _defaults = null

        // Verify that an application is specified
        if(_application==null) throw new Error("Application must be specified for serviceproperty. Required.")
        // Check that application is allowed
        if(isApplicationAllowed(_application)==false) throw new Error("Invalid application specified: " + _application)

        _propertyFile = getApplicationPropertyFile(_application) // Assemble app specific config path
        _propertyDefaultsFile = getApplicationPropertyDefaultsFile(_application) // Assemble app specific config defaults path

        // Then check if property file exists at all
        if(fileExists(_propertyFile)==false) throw new Error("Configuration file for ${_application} does not exist in expected location: ${_propertyFile}")

        if(_propertyKeys==null) throw new Error("Property key(s) to read or modify must be specified for serviceproperty. Required.")

        // Check which action to implement
        // Should a property be SET
        if(_action!=null && _action.trim().toUpperCase()=='SET') {
            if(_propertyValue==null) throw new Error("Property value to add or replace with key(s) must be specified for serviceproperty. Required.")

            setPropertyValue(_propertyFile, _propertyKeys, _propertyValue)
        }
        // Should a property line be REMOVED
        else if(_action!=null && _action.trim().toUpperCase()=='REMOVE') {
            removeProperty(_propertyFile, _propertyKeys)
        }
        // Otherwise default to READ
        else {
            _action = 'READ'
            // Only fetch defaults for a read statement
            _defaults = getPropertyKeyValues(_propertyDefaultsFile, _propertyKeys).split('\n') // Return response as array listing
        }

        // Default to echoing back current status of property manipulated
        _values = getPropertyKeyValues(_propertyFile, _propertyKeys).split('\n') // Return response as array listing

        // Make response
        response.json(HttpStatus.OK, [application: _application, action: _action, keys: _propertyKeys, values: _values, defaults: _defaults ])
    }
}

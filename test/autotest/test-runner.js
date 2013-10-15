/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

if (window.sessionStorage != null) {
    window.sessionStorage.clear();
}

// Timeout is 2 seconds to allow physical devices enough
// time to query the response. This is important for some
// Android devices.
var Tests = function() {};
Tests.TEST_TIMEOUT = 7500;

// Creates a spy that will fail if called.
function createDoNotCallSpy(name, opt_extraMessage) {
    return jasmine.createSpy().andCallFake(function() {
        var errorMessage = name + ' should not have been called.';
        if (arguments.length) {
            errorMessage += ' Got args: ' + JSON.stringify(arguments);
        }
        if (opt_extraMessage) {
            errorMessage += '\n' + opt_extraMessage;
        }
        expect(false).toBe(true, errorMessage);
    });
}

// Waits for any of the given spys to be called.
// Last param may be a custom timeout duration.
function waitsForAny() {
    var spys = [].slice.call(arguments);
    var timeout = Tests.TEST_TIMEOUT;
    if (typeof spys[spys.length - 1] == 'number') {
        timeout = spys.pop();
    }
    waitsFor(function() {
        for (var i = 0; i < spys.length; ++i) {
            if (spys[i].wasCalled) {
                return true;
            }
        }
        return false;
    }, "Expecting callbacks to be called.", timeout);
}

// Report a failure with descrition to indicate a todo job.
function todo(desc) {
    var _desc = desc ? ('+++++++ ' + desc + " +++++++") : "+++++++ This is a todo case! +++++++";
    it(_desc, function() {
        expect(false).toBeTruthy();
    });
};

// HELPER FUNCTIONS

// deletes specified file or directory
var deleteEntry = function(name, success, error) {
    // deletes entry, if it exists
    success = success || function (){console.log('deleteEntry: cleanup success.');};
    error = error || function (){console.log('deleteEntry: cleanup fail.');};
    window.resolveLocalFileSystemURI(root.toURL() + name,
        function(entry) {
            if (entry.isDirectory === true) {
                entry.removeRecursively(success, error);
            } else {
                entry.remove(success, error);
            }
        }, success);
};
// deletes file, if it exists, then invokes callback
var deleteFile = function(fileName, callback) {
    callback = callback || function (){console.log('deleteFile: cleanup success.');};
    root.getFile(fileName, null,
        // remove file system entry
        function(entry) {
            entry.remove(callback, function() { console.log('[ERROR] deleteFile cleanup method invoked fail callback.'); });
        },
        // doesn't exist
        function (){console.log('deleteFile: file does not exist.');});
};
// deletes and re-creates the specified file
var createFile = function(fileName, success, error) {
    deleteEntry(fileName, function() {
        root.getFile(fileName, {create: true}, success, error);
    }, error);
};
// deletes and re-creates the specified directory
var createDirectory = function(dirName, success, error) {
    deleteEntry(dirName, function() {
        root.getDirectory(dirName, {create: true}, success, error);
    }, error);
};

var createFail = function(module) {
    return jasmine.createSpy().andCallFake(function(err) {
        console.log('[ERROR ' + module + '] ' + JSON.stringify(err));
    });
};

var createWin = function(module) {
    return jasmine.createSpy().andCallFake(function() {
        console.log('[ERROR ' + module + '] Unexpected success callback');
    });
};

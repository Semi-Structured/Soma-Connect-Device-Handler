/**
 *  Soma Smart Shade
 *
 *  Copyright 2019 Ben A
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

metadata {
	definition (name: "Soma Smart Shade", namespace: "semi-structured", author: "Ben A", cstHandler: true) {
		capability "Window Shade"
		capability "Battery"
		capability "Switch Level"
		capability "Refresh"
		capability "Switch"
	}


	simulator {
		// TODO: define status and reply messages here
	}

  tiles(scale: 2) {
    multiAttributeTile(name:"windowShade", type: "generic", width: 6, height: 4) {
      tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
        attributeState "open", label: 'Open', action: "close", icon: "https://raw.githubusercontent.com/a4refillpad/media/master/blind-open.png", backgroundColor: "#e86d13", nextState: "closed"
        attributeState "closed", label: 'Closed', action: "open", icon: "https://raw.githubusercontent.com/a4refillpad/media/master/blind-closed.png", backgroundColor: "#00A0DC", nextState: "open"
        attributeState "partially open", label: 'Partially open', action: "close", icon: "https://raw.githubusercontent.com/a4refillpad/media/master/blind-part-open.png", backgroundColor: "#d45614", nextState: "closed"
      }
      tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
        attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
      }
    }
    standardTile("contPause", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "pause", label:"", icon:'st.sonos.stop-btn', action:'pause', backgroundColor:"#cccccc"
    }
    standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    valueTile("shadeLevel", "device.level", width: 3, height: 1) {
      state "level", label: 'Blind is ${currentValue}% open', defaultState: true
    }
    controlTile("levelSliderControl", "device.level", "slider", width:3, height: 1, inactiveLabel: false) {
      state "level", action:"switch level.setLevel"
    }
    standardTile("resetClosed", "device.windowShade", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
  		state "default", action:"close", label: "Close", icon:"https://raw.githubusercontent.com/a4refillpad/media/master/blind-closed.png"
  	}
  	standardTile("resetOpen", "device.windowShade", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
  		state "default", action:"open", label: "Open", icon:"https://raw.githubusercontent.com/a4refillpad/media/master/blind-open.png"
  	}
   	valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
  		state "battery", label:'${currentValue}% battery', unit:""
  	}

    main "windowShade"
    details(["windowShade", "shadeLevel", "levelSliderControl", "contPause", "battery", "refresh", "resetClosed", "resetOpen"])
  }
}

preferences {
		input "ip_address", "text", title: "SOMA Connect IP", required: true
		input "port", "text", title: "Port (if blank = 3000)", required: false
    input "mac_address", "text", title: "MAC Address", required: true
}


// parse callback events into attributes
void callBackHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Entered callBackHandler()..."
	def json = hubResponse.json
	log.debug "Parsing '${json}'"

	// If command was executed successfully
	if (json.result == "success") {
		log.debug "Command executed successfully"

		// If response is for battery level
		if (json.battery_level){
			// represent batter level as a percentage
			def battery_percent = json.battery_level - 320
			if (battery_percent > 100) {battery_percent = 100}
			sendEvent(name: "battery", value: battery_percent)
		}
		// If response is for shade level
		else if (json.find{ it.key == "position" }){
			def new_level = 100 - json.position  // represent level as % open
			sendEvent(name: "level", value: new_level)

			// Update shade state
			if (new_level == 100){
				sendEvent(name: "windowShade", value: "open")
			} else if (new_level == 0) {
				sendEvent(name: "windowShade", value: "closed")
			} else {
				sendEvent(name: "windowShade", value: "partially open")
			}
		}
		// If successfull response is from another action, get new shade level
		else {
			getLevel()
		}
	}
}


// handle commands
def open() {
  log.debug "Executing OPEN"
  if (ip_address){
		def port
		if (port){
			port = "${port}"
		} else {
			port = 3000
		}

		def result = new physicalgraph.device.HubAction(
			method: "GET",
			path: "/open_shade/${mac_address}",
			headers: [
			HOST: "${ip_address}:${port}"
			],
			device.deviceNetworkId,
			[callback: callBackHandler]
		)
		sendHubCommand(result)
		sendEvent(name: "shade", value: "open")
    log.debug result
	}
}

def close() {
  log.debug "Executing CLOSE"
  if (ip_address){
    def port
    if (port){
      port = "${port}"
    } else {
      port = 3000
    }

  def result = new physicalgraph.device.HubAction(
    method: "GET",
    path: "/close_shade/${mac_address}",
    headers: [
      HOST: "${ip_address}:${port}"
    ],
		device.deviceNetworkId,
		[callback: callBackHandler]
  )
  sendHubCommand(result)
  sendEvent(name: "shade", value: "close")
  log.debug result
  }
}

def pause() {
  log.debug "Executing STOP"
  if (ip_address){
    def port
    if (port){
      port = "${port}"
    } else {
      port = 3000
    }

  def result = new physicalgraph.device.HubAction(
    method: "GET",
    path: "/stop_shade/${mac_address}",
    headers: [
      HOST: "${ip_address}:${port}"
    ],
		device.deviceNetworkId,
		[callback: callBackHandler]
  )
  sendHubCommand(result)
  sendEvent(name: "shade", value: "stopped")
  log.debug result
  }
}

def setLevel(data) {
  log.debug "Executing SET LEVEL"
  if (ip_address){
    def port
    if (port){
      port = "${port}"
    } else {
      port = 3000
    }

  data = data.toInteger()
	def open_level = 100 - data

  def result = new physicalgraph.device.HubAction(
    method: "GET",
    path: "/set_shade_position/${mac_address}/${open_level}",
    headers: [
      HOST: "${ip_address}:${port}"
    ],
		device.deviceNetworkId,
		[callback: callBackHandler]
  )
  sendHubCommand(result)
  sendEvent(name: "shade", value: "set level ${data}")
  log.debug result
  }
}

def getLevel() {
  log.debug "Executing GET SHADE LEVEL"
  if (ip_address){
    def port
    if (port){
      port = "${port}"
    } else {
      port = 3000
    }

  def result = new physicalgraph.device.HubAction(
    method: "GET",
    path: "/get_shade_state/${mac_address}",
    headers: [
      HOST: "${ip_address}:${port}"
    ],
		device.deviceNetworkId,
		[callback: callBackHandler]
  )
  sendHubCommand(result)
  sendEvent(name: "shade", value: "get level")
  log.debug result
  }
}

def batteryLevel() {
  log.debug "Executing GET BATTERY LEVEL"
  if (ip_address){
    def port
    if (port){
      port = "${port}"
    } else {
      port = 3000
    }

  def result = new physicalgraph.device.HubAction(
    method: "GET",
    path: "/get_battery_level/${mac_address}",
    headers: [
      HOST: "${ip_address}:${port}"
    ],
		device.deviceNetworkId,
		[callback: callBackHandler]
  )
  sendHubCommand(result)
  sendEvent(name: "shade", value: "get battery level")
  log.debug result
  }
}

def refresh() {
	getLevel()
	batteryLevel()
}

// Switch capability allows blinds to emulate a switch for compatibility with Google Assistant, etc
def on() {
	open()
}

def off() {
	close()
}

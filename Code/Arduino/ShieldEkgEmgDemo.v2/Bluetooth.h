
/*  ----------------------------------------------------------------------------
	FILE: 	 Bluetooth.h
	PROJECT: MOD-BT Interface with Arduino boards
	
	Pinguino library modified by Olimex for Arduino 1.0.3
	
	History:
	Version 1.0 - January 31, 2013
				  Initial version.
	
	If you have any questions, email
	support@olimex.com
	https://www.olimex.com	
	
/*	--- From Pinguino IDE: -----------------------------------------------------
	PROJECT:		pinguino
	PURPOSE:		BGB203 basic functions
	PROGRAMER:		regis blabnchot <rblanchot@gmail.com>
	FIRST RELEASE:	28 oct. 2011
	LAST RELEASE:	04 mar. 2012
	----------------------------------------------------------------------------
	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
	--------------------------------------------------------------------------*/

#ifndef __BLUETOOTH_H
#define __BLUETOOTH_H

#include <stdlib.h>				// malloc, ...
#include <stdarg.h>				// variable args
#include <string.h>				// strlen, ...
#include <typedef.h>			// unsigned char, u32, ... definitions
#include <Arduino.h>
#include <HardwareSerial.h>		// Serial

#define BT_DELAY		10		// Delay (ms) before sending a new command to the BT module
#define CRLF			"/r/n"	// <CR><LF>

// BT return codes
typedef enum
{
	BT_OK		=	111,
	BT_ERROR,
	BT_COMPLETE
} BT_STATUS;

// BT response
typedef struct
{
	char *command;
	char *data;
	char *status;			// OK or ERROR
	BT_STATUS code;		// BT_OK or BT_ERROR
} BT_RESPONSE;

//	----------------------------------------------------------------------------
//	Get a complete response from the module
//	----------------------------------------------------------------------------

// char *BT_getBuffer(unsigned char *buffer)
void BT_getBuffer(char * buffer)
{
	unsigned char c, i = 0;
	
	// wait until something is received
	while (!Serial.available());

	do {
		c = Serial.read();
		if (c != 255) buffer[i++] = c;
	} while (c != 255);// && (i < 80) );
	// C string must be null-terminated
	//BT_BUFFER[i] = '\0';
	buffer[i] = '\0';

}

//	----------------------------------------------------------------------------
//	Get status response
//	----------------------------------------------------------------------------

BT_STATUS BT_getStatusCode(char * status)
{
	unsigned char length;
	BT_STATUS code;
	
	length = strlen(status);
	
	// check last char if status is OK, ERROR or COMPLETE
	switch (status[length-1])
	{
		case 'K': return BT_OK;
		case 'R': return BT_ERROR;
		case 'E': return BT_COMPLETE;
	}
}

//	----------------------------------------------------------------------------
//	Get a short response from the module
//	<CR><LF><status><CR><LF>
//	with status = OK or ERROR
//	----------------------------------------------------------------------------

BT_RESPONSE BT_getResponse(BT_RESPONSE response)
{
	char buffer[128];

	// get a complete response from the module
	BT_getBuffer(buffer);

	response.command = NULL;
	response.data    = strtok(buffer, CRLF); // to remove the first CRLF 
	response.data    = strtok(NULL, CRLF);
	response.status  = strtok(NULL, CRLF);
	response.code    = BT_getStatusCode(response.status);

	return response;
}

//	----------------------------------------------------------------------------
//	Get an extended response from the module
//	<CR><LF><command><delimiter><data><CR><LF><status><CR><LF>
//	----------------------------------------------------------------------------

BT_RESPONSE BT_getExtendedResponse()
{
	unsigned char length;
	unsigned char *buffer;
	BT_RESPONSE response;
	
	// get a complete response from the module
	//buffer = BT_getBuffer(, buffer);

	// response's format is :
	//    <CR><LF><status><CR><LF> with status = OK or ERROR
/*	
	response.command = strtok(buffer, CRLF); // to remove the first CRLF 
	response.command = strtok(buffer, CRLF);
	response.data    = strtok(NULL, CRLF);
	response.status  = strtok(NULL, CRLF);
	response.code    = BT_getStatusCode(response.status);

	return response;
*/
}

//	----------------------------------------------------------------------------
//	Send an AT command to the module over the UART Port
//	----------------------------------------------------------------------------

BT_RESPONSE BT_sendCommand(const char *fmt, ...)
{
	Serial.flush();
	/**********************************************/
	/** http://playground.arduino.cc/Main/Printf **/
		
		char tmp[128]; // resulting string limited to 128 chars
		va_list args;
		va_start (args, fmt );
		vsnprintf(tmp, 128, fmt, args);
		va_end (args);
		Serial.print(tmp);
		
	/***********************************************/
	delay(BT_DELAY);
}

//	----------------------------------------------------------------------------
//	Get into command mode
//	----------------------------------------------------------------------------

BT_RESPONSE BT_setCommandMode()
{
	BT_sendCommand("+++\r");
	BT_sendCommand("+++\r");
	BT_sendCommand("+++\r");
	//return BT_getResponse();
}

//	----------------------------------------------------------------------------
//	Configures the device not to echo received characters in command mode
//	----------------------------------------------------------------------------

BT_RESPONSE BT_echoOff()
{
	BT_sendCommand("ATE0\r");
	//return BT_getResponse();
}

//	----------------------------------------------------------------------------
//	Configures the device to echo received characters in command mode
//	----------------------------------------------------------------------------

BT_RESPONSE BT_echoOn()
{
	BT_sendCommand("ATE1\r");
	//return BT_getResponse();
}

//	----------------------------------------------------------------------------
//	Restore the current configuration settings back to the settings
//	that were stored by the Factory Settings tool
//	or settings that were stored to Flash.
//	----------------------------------------------------------------------------

BT_RESPONSE BT_restore()
{
	BT_sendCommand("AT&F\r");
	//return BT_getResponse();
}

//	----------------------------------------------------------------------------
//	Restore the current configuration settings back to internal default values
//	----------------------------------------------------------------------------

BT_RESPONSE BT_reset()
{
	BT_sendCommand("ATZ\r");
	//return BT_getResponse();
}

//	----------------------------------------------------------------------------
//	Ask the module for its firmware version 
//	----------------------------------------------------------------------------

BT_RESPONSE BT_getFirmware()
{
	BT_sendCommand("ATI\r");
	//return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Change module name
//	----------------------------------------------------------------------------

BT_RESPONSE BT_setDeviceName(char * name)
{
	BT_sendCommand("AT+BTLNM=\"%s\"\r", name); 
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Get module name
//	----------------------------------------------------------------------------

BT_RESPONSE BT_getDeviceName()
{
	BT_sendCommand("AT+BTLNM?\r");
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Set bluetooth device address (0x01)
//	----------------------------------------------------------------------------

BT_RESPONSE BT_setDeviceAddress(char * bdaddr)
{
	BT_sendCommand("AT+BTSET=1,%s\r", bdaddr);
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Get bluetooth device address
//	----------------------------------------------------------------------------

BT_RESPONSE BT_getDeviceAddress()
{
	BT_sendCommand("AT+BTBDA?\r");
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Allows automatic Bluetooth connection
//	----------------------------------------------------------------------------

BT_RESPONSE BT_setAutoConnection()
{
	BT_sendCommand("AT+BTAUT=1,0\r");
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	
//	----------------------------------------------------------------------------

BT_RESPONSE BT_setUARTSpeed(unsigned long long baud_rate)
{
	char *string;

	// Change UART settings (baud rate, data bits, stop bits, parity, stop bits, flow control)
	// Enable RTS/CTS, DTR/DSR Flow control
	BT_sendCommand("AT+BTURT=%s,8,0,1,3\r", ultoa(baud_rate, string, 10));
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Disable security process when pairing
//	----------------------------------------------------------------------------

BT_RESPONSE BT_disableSecurity()
{
	BT_sendCommand("AT+BTSEC=0\r");
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Writes above setting to the BGB203 flash memory
//	----------------------------------------------------------------------------

BT_RESPONSE BT_writeFlash()
{
	BT_sendCommand("AT+BTFLS\r");
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Start Bluetooth Server
//	----------------------------------------------------------------------------

BT_RESPONSE BT_start()
{
	BT_sendCommand("AT+BTSRV=1\r");
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Enter deep sleep mode
//	----------------------------------------------------------------------------

BT_RESPONSE BT_sleep()
{
	BT_sendCommand("AT+BTSLP\r");
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Search for new devices for s seconds
//	----------------------------------------------------------------------------

BT_RESPONSE BT_search(char s)
{
	// cancels any current commands
	BT_sendCommand("AT+BTCAN\r");
	BT_sendCommand("AT+BTINQ=%d\r", s);
	return BT_getExtendedResponse();
}

//	----------------------------------------------------------------------------
//	Initialize the BGB203 Bluetooth module
//	return : name of the device (Pinguino)
//	----------------------------------------------------------------------------

BT_RESPONSE BT_init(unsigned long long baud_rate)
{
	BT_RESPONSE response;

	// 115200 bauds is the default configuration value
	//SerialConfigure(UART_ENABLE, UART_RX_TX_ENABLED,	115200);

	response = BT_setCommandMode(); 
	//BT_getFirmware();
	//BT_echoOff();
	//BT_restore();
	//BT_setDeviceName("Pinguino");
	// Pass through, DCE, enable escape sequence, disable entering in command mode with DTR/DSR, enable LED
	//BT_sendCommand("AT+BTCFG=33");
	//BT_setAutoConnection();

	// if (baud_rate != 115200)
	// {
		// BT_setUARTSpeed(baud_rate);
		// new UART speed
		// SerialConfigure(UART_ENABLE, UART_RX_TX_ENABLED, baud_rate);
	// }

	//BT_disableSecurity();
	//BT_writeFlash();
	//BT_start();
	return response;
}

//	----------------------------------------------------------------------------
//
// 0x00 Delete any stored link key
// 0x01 Pair with remote device (initiate)
// 0x02 Allow another device to pair (wait for pair)
// 0x03 Configure Bluetooth address/Link key pair
//	----------------------------------------------------------------------------

BT_RESPONSE BT_connect(unsigned char * bdaddr)
{
	BT_RESPONSE response;
	
	BT_sendCommand("AT+BTPAR=1,%s\r", bdaddr); // Pair with remote device
	response = BT_getExtendedResponse();
	if (response.code == BT_ERROR) return response;
	BT_sendCommand("AT+BTCLT=%s,1\r", bdaddr); // on Port 1
	response = BT_getExtendedResponse();
	// the device will enter data mode if OK
	return response;
}

#endif /* __BLUETOOTH_H */

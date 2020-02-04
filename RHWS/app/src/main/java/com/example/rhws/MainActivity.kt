package com.example.rhws

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Button
import android.widget.Toast
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    internal var btnCurr: Button? = null
    internal var btnDesired: Button? = null

    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    private var outStream: OutputStream? = null

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCurr = findViewById<Button>(R.id.btnCurrentTempDisplay)
        btnDesired = findViewById<Button>(R.id.btnDesiredTempDisplay)


        btAdapter = BluetoothAdapter.getDefaultAdapter()
        checkBTState()

        btnCurr!!.setOnClickListener{
            tempChange("1")
            Toast.makeText(baseContext, "Turn LED ON: Sending Char 1", Toast.LENGTH_SHORT).show()
        }

        btnDesired!!.setOnClickListener{
            tempChange("0")
            Toast.makeText(baseContext, "Turn LED OFF: Sending Char 0 ", Toast.LENGTH_SHORT).show()
        }

    }

    private fun errorExit(title: String, message: String) {
        Toast.makeText(baseContext, "$title - $message", Toast.LENGTH_LONG).show()
        finish()
    }
    private fun checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth is not supported")
        } else {
            if (btAdapter!!.isEnabled) {
                Log.d(TAG, "Bluetooth is ON...")
            } else {
                //Prompt user to turn on Bluetooth
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, 1)
            }
        }
    }

    fun createBluetoothSocket(device: BluetoothDevice): Any? {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                val m = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", *arrayOf<Class<*>>(UUID::class.java))
                return m.invoke(device, MY_UUID)
            } catch (e: Exception) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e)
            }

        }
        return device.createRfcommSocketToServiceRecord(MY_UUID)
    }

    public override fun onResume() {
        super.onResume()

        Log.d(TAG, "onResume(): Creating bluetooth socket ...")

        // Set up a pointer to the remote node using it's address.
        val device = btAdapter!!.getRemoteDevice(address)

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device) as BluetoothSocket?
            Log.d(TAG, "onResume(): Bluetooth socket created ...")
        } catch (e1: IOException) {
            errorExit("Fatal Error", "onResume(): Create bluetooth socket FAILED: " + e1.message + ".")
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter!!.cancelDiscovery()

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "Connecting to Bluetooth Device ...")
        try {
            btSocket!!.connect()
            Log.d(TAG, "Bluetooth Device Connected ...")
        } catch (e: IOException) {
            try {
                btSocket!!.close()
            } catch (e2: IOException) {
                errorExit("Fatal Error", "onResume(): Unable to close socket when closing connection: " + e2.message + ".")
            }

        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "onResume(): Creating data output stream ...")

        try {
            outStream = btSocket!!.outputStream
        } catch (e: IOException) {
            errorExit("Fatal Error", "onResume(): Creating data output stream FAILED: " + e.message + ".")
        }

    }

    private fun tempChange(message: String){
        val msgBuffer = message.toByteArray()

        Log.d(TAG, "Sending data: $message...")

        try {
            outStream!!.write(msgBuffer)
        } catch (e: IOException) {
            var msg = "onResume(): Exception occurred during write: " + e.message
            if (address == "00:00:00:00:00:00")
                msg = "$msg.\n\nChange your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code"
            msg = "$msg.\n\nCheck that the SPP UUID: $MY_UUID exists on server.\n\n"

            errorExit("Fatal Error", msg)
        }
    }

    companion object {
        private val TAG = "BT Send Char"

        // SPP UUID service. DO NOT CHANGE!!! This is a Standard SerialPortService ID per
        // https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html#createRfcommSocketToServiceRecord%28java.util.UUID%29
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // MAC-address of Adafruit Bluefruit EZ-Link module (you must edit this line)
        private val address = "98:76:B6:00:9D:92"
    }
}

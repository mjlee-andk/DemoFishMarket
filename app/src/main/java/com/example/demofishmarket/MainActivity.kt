package com.example.demofishmarket

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.Toast
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothState
import app.akexorcist.bluetotohspp.library.DeviceList
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var mScan: Button
    private lateinit var mPrint: Button
    private lateinit var mDisc: Button
    private lateinit var mBtnSave: Button
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val applicationUUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var mBluetoothConnectProgressDialog: ProgressDialog? = null
    private var mBluetoothSocket: BluetoothSocket? = null
    private lateinit var mBluetoothDevice: BluetoothDevice

    private lateinit var mData: ByteArray

    private val TAG: String = "Main"

    private var bt: BluetoothSPP? = null

    var os: OutputStreamWriter? = null

    private var scaleArray = ArrayList<Float>()

    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            mBluetoothConnectProgressDialog!!.dismiss()
            Toast.makeText(this@MainActivity, "DeviceConnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate")

        bt = BluetoothSPP(applicationContext)

        // 블루투스 사용가능여부 체크
        if(bt?.isBluetoothAvailable == false) {
            Log.d(TAG, "Bluetooth is not available")
            finish()
        }

        Log.d(TAG, "Bluetooth is available")

        bt?.setOnDataReceivedListener { data, message ->
            // 데이터 수신
            mData = data
//            textView.text = message
        }

        bt?.setBluetoothConnectionListener(object : BluetoothSPP.BluetoothConnectionListener {
            // 연결됐을 때
            override fun onDeviceConnected(name: String, address: String) {
                Toast.makeText(applicationContext
                    , "Connected to $name\n$address"
                    , Toast.LENGTH_SHORT).show()
            }

            // 연결해제
            override fun onDeviceDisconnected() {
                Toast.makeText(applicationContext
                    , "Connection lost", Toast.LENGTH_SHORT).show()
            }

            // 연결실패
            override fun onDeviceConnectionFailed() {
                Toast.makeText(applicationContext
                    , "Unable to connect", Toast.LENGTH_SHORT).show()
            }
        })

        // 저울 블루투스 통신 연결
        btnConnect.setOnClickListener {
            when (it.id) {
                R.id.btnConnect -> {
                    if(bt?.serviceState == BluetoothState.STATE_CONNECTED) {
                        bt?.disconnect()
                    } else {
                        val intent: Intent = Intent(applicationContext, DeviceList::class.java)
                        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE)
                    }
                }
            }
        }

        mScan = findViewById<Button>(R.id.Scan)
        mScan.setOnClickListener {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter == null) {
                Toast.makeText(this@MainActivity, "Message1", Toast.LENGTH_SHORT).show()
            } else {
                if (!mBluetoothAdapter!!.isEnabled) {
                    val enableBtIntent = Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent,
                            REQUEST_ENABLE_BT)
                } else {
                    ListPairedDevices()
                    val connectIntent = Intent(this@MainActivity,
                            DeviceListActivity::class.java)
                    startActivityForResult(connectIntent,
                            REQUEST_CONNECT_DEVICE)
                }
            }
        }

        // 계량 취득
        mBtnSave = findViewById<Button>(R.id.btnSave)
        mBtnSave.setOnClickListener {
            try {
                var scaleStr = String(mData, Charsets.UTF_8).substring(3)
                var scale = scaleStr.toFloat() / 100
                scaleArray.add(scale)
                Log.d("ByteArray to String", scaleStr)

                textView.text = String.format("%.2f", getTotalScale(scaleArray))
            } catch(e: Exception) {

            }
        }

        mPrint = findViewById<Button>(R.id.mPrint)
        mPrint.setOnClickListener {
            //            val t = object : Thread() {
//                override fun run() {
//                    try {
////                        val os = mBluetoothSocket!!
////                                .outputStream
//
//                        var BILL = ""
//
//                        BILL = ("                   XXXX MART    \n"
//                                + "                   XX.AA.BB.CC.     \n " +
//                                "                 NO 25 ABC ABCDE    \n" +
//                                "                  XXXXX YYYYYY      \n" +
//                                "                   MMM 590019091      \n")
//                        BILL = BILL + "-----------------------------------------------\n"
//
//
//                        BILL = BILL + String.format("%1$-10s %2$10s %3$13s %4$10s", "Item", "Qty", "Rate", "Totel")
//                        BILL = BILL + "\n"
//                        BILL = BILL + "-----------------------------------------------"
//                        BILL = BILL + "\n " + String.format("%1$-10s %2$10s %3$11s %4$10s", "item-001", "5", "10", "50.00")
//                        BILL = BILL + "\n " + String.format("%1$-10s %2$10s %3$11s %4$10s", "item-002", "10", "5", "50.00")
//                        BILL = BILL + "\n " + String.format("%1$-10s %2$10s %3$11s %4$10s", "item-003", "20", "10", "200.00")
//                        BILL = BILL + "\n " + String.format("%1$-10s %2$10s %3$11s %4$10s", "item-004", "50", "10", "500.00")
//
//                        BILL = BILL + "\n-----------------------------------------------"
//                        BILL = BILL + "\n\n "
//
//                        BILL = BILL + "                   Total Qty:" + "      " + "85" + "\n"
//                        BILL = BILL + "                   Total Value:" + "     " + "700.00" + "\n"
//
//                        BILL = BILL + "-----------------------------------------------\n"
//                        BILL = BILL + "\n\n "
////                        os.write(BILL.toByteArray())
//                        //This is printer specific code you can comment ==== > Start
//
//
//                        os?.write("한글제발")
//                        os?.flush()
//                        // Setting height
////                        val gs = 29
////                        os.write(intToByteArray(gs).toInt())
////                        val h = 104
////                        os.write(intToByteArray(h).toInt())
////                        val n = 162
////                        os.write(intToByteArray(n).toInt())
////
////                        // Setting Width
////                        val gs_width = 29
////                        os.write(intToByteArray(gs_width).toInt())
////                        val w = 119
////                        os.write(intToByteArray(w).toInt())
////                        val n_width = 2
////                        os.write(intToByteArray(n_width).toInt())
//
//
//                    } catch (e: Exception) {
//                        Log.e("MainActivity", "Exe ", e)
//                    }
//
//                }
//            }
//            t.start()
            try {
                os?.write("가나다라마바사아자카타파하abcdefghijklmnopqrstuvwxyz1234567890`~!@#$%^&*()_+{}][;:'\\|,.<>?/")
                os?.flush()
            } catch(e: Exception) {
                Log.e("MAIN", "msg error ", e)
            }

        }

//        mDisc = findViewById<Button>(R.id.dis)
//        mDisc.setOnClickListener {
//            if (mBluetoothAdapter != null)
//                mBluetoothAdapter!!.disable()
//        }
    }

    fun getTotalScale(data: ArrayList<Float>): Float {
        var result:Float = 0.0F
        for(d in data) {
            result += d
        }

        return result
    }

    override fun onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        try {
            bt?.stopService()
            if (mBluetoothSocket != null)
                mBluetoothSocket!!.close()
        } catch (e: Exception) {
            Log.e("Tag", "Exe ", e)
        }

    }

    override fun onBackPressed() {
        try {
            if (mBluetoothSocket != null)
                mBluetoothSocket!!.close()
        } catch (e: Exception) {
            Log.e("Tag", "Exe ", e)
        }

        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")

        // 블루투스 꺼져있을 때
        if (bt?.isBluetoothEnabled == false) { //
            val i: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (bt?.isServiceAvailable == false) {
                bt?.setupService();
                bt?.startService(BluetoothState.DEVICE_OTHER);
                setup();
            }
        }
    }

    override fun onActivityResult(mRequestCode: Int, mResultCode: Int, mDataIntent: Intent?) {
        super.onActivityResult(mRequestCode, mResultCode, mDataIntent)
        Log.d(TAG, "onActivityResult")

        when (mRequestCode) {
            // 프린터 연결
            REQUEST_CONNECT_DEVICE -> if (mResultCode == Activity.RESULT_OK) {
                val mExtra = mDataIntent?.extras
                val mDeviceAddress = mExtra!!.getString("DeviceAddress")
                Log.v(TAG, "Coming incoming address " + mDeviceAddress!!)
                mBluetoothDevice = mBluetoothAdapter!!
                        .getRemoteDevice(mDeviceAddress)
                mBluetoothConnectProgressDialog = ProgressDialog.show(this,
                        "Connecting...", mBluetoothDevice.name + " : "
                        + mBluetoothDevice.address, true, false)
                val mBlutoothConnectThread = Thread(Runnable{
                    try {
                        mBluetoothSocket = mBluetoothDevice
                                .createRfcommSocketToServiceRecord(applicationUUID)
                        mBluetoothAdapter!!.cancelDiscovery()
                        mBluetoothSocket!!.connect()
                        os = OutputStreamWriter(mBluetoothSocket!!.outputStream, "EUC-KR")
                        mHandler.sendEmptyMessage(0)
                    } catch (eConnectException: IOException) {
                        Log.d(TAG, "CouldNotConnectToSocket", eConnectException)
                        closeSocket(this!!.mBluetoothSocket!!)
                    }
                })
                mBlutoothConnectThread.start()
                // pairToDevice(mBluetoothDevice); This method is replaced by
                // progress dialog with thread
            }

            REQUEST_ENABLE_BT -> if (mResultCode == Activity.RESULT_OK) {
                ListPairedDevices()
                val connectIntent = Intent(this@MainActivity,
                        DeviceListActivity::class.java)
                startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE)
            }

            // 저울 연결
            BluetoothState.REQUEST_CONNECT_DEVICE -> if(mResultCode == Activity.RESULT_OK) {
                bt?.connect(mDataIntent)
            }

            BluetoothState.REQUEST_ENABLE_BT -> if(mResultCode == Activity.RESULT_OK) {
                bt?.setupService()
                bt?.startService(BluetoothState.DEVICE_OTHER)
                setup()
            }
            else {
                Toast.makeText(this@MainActivity, "Message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setup() {
        val btnSend = findViewById<Button>(R.id.btnSend); //데이터 전송
        btnSend.setOnClickListener{
            when(it.id) {
                R.id.btnSend -> {
                    bt?.send("Text", true)
                }
            }
        }
    }

    private fun ListPairedDevices() {
        val mPairedDevices = mBluetoothAdapter!!
                .bondedDevices
        if (mPairedDevices.size > 0) {
            for (mDevice in mPairedDevices) {
                Log.v(TAG, "PairedDevices: " + mDevice.name + "  "
                        + mDevice.address)
            }
        }
    }

    fun run() {
        try {
            mBluetoothSocket = mBluetoothDevice
                    .createRfcommSocketToServiceRecord(applicationUUID)
            mBluetoothAdapter!!.cancelDiscovery()
            mBluetoothSocket!!.connect()
            mHandler.sendEmptyMessage(0)
        } catch (eConnectException: IOException) {
            Log.d(TAG, "CouldNotConnectToSocket", eConnectException)
            closeSocket(this!!.mBluetoothSocket!!)
            return
        }

    }

    private fun closeSocket(nOpenSocket: BluetoothSocket) {
        try {
            nOpenSocket.close()
            Log.d(TAG, "SocketClosed")
        } catch (ex: IOException) {
            Log.d(TAG, "CouldNotCloseSocket")
        }

    }

    fun sel(`val`: Int): ByteArray {
        val buffer = ByteBuffer.allocate(2)
        buffer.putInt(`val`)
        buffer.flip()
        return buffer.array()
    }

    companion object {
        protected val TAG = "TAG"
        private val REQUEST_CONNECT_DEVICE = 1
        private val REQUEST_ENABLE_BT = 2

        fun intToByteArray(value: Int): Byte {
            val b = ByteBuffer.allocate(4).putInt(value).array()

//            for (k in b.indices) {
//                println("Selva  [" + k + "] = " + "0x"
//                        + UnicodeFormatter.byteToHex(b[k]))
//            }

            return b[3]
        }
    }
}

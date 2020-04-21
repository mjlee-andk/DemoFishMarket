package com.example.demofishmarket

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothState
import app.akexorcist.bluetotohspp.library.DeviceList
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
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
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // 블루투스 사용가능여부 체크
        if(bt?.isBluetoothAvailable == false) {
            Log.d(TAG, "Bluetooth is not available")
            finish()
        }

        Log.d(TAG, "Bluetooth is available")

        // SharedPreferences 초기화
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()

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
                Prefs.putString(getString(R.string.scale_name), name)
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
//            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter == null) {
                Toast.makeText(this@MainActivity, "Message1", Toast.LENGTH_SHORT).show()
            } else {
                if (!mBluetoothAdapter!!.isEnabled) {
                    val enableBtIntent = Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent,
                            REQUEST_ENABLE_BT)
                } else {
                    listPairedDevices()
                    val connectIntent = Intent(this@MainActivity,
                            DeviceListActivity::class.java)
                    startActivityForResult(connectIntent,
                            REQUEST_CONNECT_DEVICE)
                }
            }
        }

        var listView = findViewById<ListView>(R.id.listView)
        var myAdapter = MyAdapter(this, scaleArray);

        listView.adapter = myAdapter;

        // 계량 취득
        mBtnSave = findViewById<Button>(R.id.btnSave)
        mBtnSave.setOnClickListener {
            try {
                var scaleStr = String(mData, Charsets.UTF_8).substring(3)
                var scale = scaleStr.toFloat() / 100

                if(scale == 0.0f) {
                    Toast.makeText(this@MainActivity, "No Data", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                scaleArray.add(scale)
                Log.d("ByteArray to String", scaleStr)
                listView.setSelection(listView.adapter.count - 1); // 아이템 추가시 최하단으로 내리기 위한 코드
                myAdapter.notifyDataSetChanged()
                textView.text = String.format("%.2f", getTotalScale(scaleArray))

            } catch(e: Exception) {

            }
        }

        mPrint = findViewById<Button>(R.id.mPrint)
        mPrint.setOnClickListener {
            if(getTotalScale(scaleArray) == 0.0f) {
                Toast.makeText(this@MainActivity, "No Data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                var storeName = "여백수산" // 업체명
                var storeBusinessNumber = "123-45-67890" // 사업자번호
                var storeAddress = "서울특별시 영등포구 국제금융로6길 33 (여의도동) 여의도백화점 8층" // 주소
                var storePhone = "02)780-4101" // 전화번호
                var storeFax = "02)782-4280,4264" // 팩스

                var date = Date();
                var simpleDateFormat = SimpleDateFormat("yyyy/MM/dd, hh:mm:ss") // 거래일시
                var partnerStore = "오징어바다 여의도점" // 거래업체
                var fishSpecies = "광어" // 거래어종
                var totalScale = textView.text.toString() // 총 중량

                var bill = ""
                bill += String.format("%16s", "거래내역전표")
                bill += "\n--------------------------------"
                bill = bill + "\n " + String.format("%1$-10s%2$16s", storeName, storeBusinessNumber)
                bill = "$bill\n $storeAddress"
                bill = "$bill\n TEL: $storePhone\n" +
                        " FAX: $storeFax"
                bill += "\n--------------------------------"
                bill = bill + "\n " + "거래일시: " + simpleDateFormat.format(date).toString()
                bill = "$bill\n 거래업체: $partnerStore"
                bill = "$bill\n 거래어종: $fishSpecies"
                bill = "$bill\n 총  중량: $totalScale" + "kg"
                bill += "\n--------------------------------\n\n\n\n\n"

                os?.write(bill)
                os?.flush()

                // 인쇄 후 초기화
                textView.text = String.format("%.1f", 0.0)
                scaleArray.clear()
                myAdapter.notifyDataSetChanged()
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
            bt?.stopService()
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
            // 저울, 기기간 블루투스 연결 여부 체크
            if (bt?.isServiceAvailable == false) {
                bt?.setupService()
                bt?.startService(BluetoothState.DEVICE_OTHER)

                var scaleName = Prefs.getString(getString(R.string.scale_name), null)
                if(scaleName != null) {
                    bt?.autoConnect(scaleName)
                }
                setup();
            }
            // 프린터, 기기간 블루투스 연결 여부 체크
            if(mBluetoothSocket == null) {
                var printerAddress = Prefs.getString(getString(R.string.printer_address), null)
                if(printerAddress != null) {
                    val mDeviceAddress = printerAddress
                    Log.v(TAG, "Coming incoming address " + mDeviceAddress!!)
                    mBluetoothDevice = mBluetoothAdapter!!
                        .getRemoteDevice(mDeviceAddress)
//                    mBluetoothConnectProgressDialog = ProgressDialog.show(this,
//                        "Connecting...", mBluetoothDevice.name + " : "
//                                + mBluetoothDevice.address, true, false)
                    val mBluetoothConnectThread = Thread(Runnable{
                        try {
                            mBluetoothSocket = mBluetoothDevice
                                .createRfcommSocketToServiceRecord(applicationUUID)
                            mBluetoothAdapter!!.cancelDiscovery()
                            mBluetoothSocket!!.connect()
                            os = OutputStreamWriter(mBluetoothSocket!!.outputStream, getString(R.string.EUC_KR))
//                            mHandler.sendEmptyMessage(0)
                        } catch (eConnectException: IOException) {
                            Log.d(TAG, "CouldNotConnectToSocket", eConnectException)
                            closeSocket(this!!.mBluetoothSocket!!)
                        }
                    })
                    mBluetoothConnectThread.start()
                }
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
                Prefs.putString(getString(R.string.printer_address), mDeviceAddress)
                Log.v(TAG, "Coming incoming address " + mDeviceAddress!!)
                mBluetoothDevice = mBluetoothAdapter!!
                        .getRemoteDevice(mDeviceAddress)
                mBluetoothConnectProgressDialog = ProgressDialog.show(this,
                        "Connecting...", mBluetoothDevice.name + " : "
                        + mBluetoothDevice.address, true, false)
                val mBluetoothConnectThread = Thread(Runnable{
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
                mBluetoothConnectThread.start()
                // pairToDevice(mBluetoothDevice); This method is replaced by
                // progress dialog with thread
            }

            REQUEST_ENABLE_BT -> if (mResultCode == Activity.RESULT_OK) {
                listPairedDevices()
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

    private fun getTotalScale(data: ArrayList<Float>): Float {
        var result:Float = 0.0F
        if(data.size == 0) {
            return result
        }
        for(d in data) {
            result += d
        }

        return result
    }

    private fun setup() {
        val btnSend = findViewById<Button>(R.id.btnSend); //데이터 전송
        btnSend.setOnClickListener{
            when(it.id) {
                R.id.btnSend -> {
                    bt?.send("Text", true)
                }
            }
        }
    }

    private fun listPairedDevices() {
        val mPairedDevices = mBluetoothAdapter!!
                .bondedDevices
        if (mPairedDevices.size > 0) {
            for (mDevice in mPairedDevices) {
                Log.v(TAG, "PairedDevices: " + mDevice.name + "  "
                        + mDevice.address)
            }
        }
    }

    private fun run() {
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

    private fun sel(`val`: Int): ByteArray {
        val buffer = ByteBuffer.allocate(2)
        buffer.putInt(`val`)
        buffer.flip()
        return buffer.array()
    }

    companion object {
        protected val TAG = "TAG"
        private const val REQUEST_CONNECT_DEVICE = 1
        private const val REQUEST_ENABLE_BT = 2

        fun intToByteArray(value: Int): Byte {
            val b = ByteBuffer.allocate(4).putInt(value).array()

//            for (k in b.indices) {
//                println("Selva  [" + k + "] = " + "0x"
//                        + UnicodeFormatter.byteToHex(b[k]))
//            }

            return b[3]
        }
    }

    class MyAdapter(context: Context?, data: ArrayList<Float>) :
        BaseAdapter() {
        var mContext: Context? = null
        var mLayoutInflater: LayoutInflater? = null
        var sample: ArrayList<Float>
        override fun getCount(): Int {
            return sample.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Float {
            return sample[position]
        }

        override fun getView(position: Int, converView: View?, parent: ViewGroup?): View? {
            val view: View? = mLayoutInflater?.inflate(R.layout.listview_custom, null)
            val scaleValue = view?.findViewById(R.id.scaleValue) as TextView
            scaleValue.text = sample[position].toString()
            return view
        }

        init {
            mContext = context
            sample = data
            mLayoutInflater = LayoutInflater.from(mContext)
        }
    }
}

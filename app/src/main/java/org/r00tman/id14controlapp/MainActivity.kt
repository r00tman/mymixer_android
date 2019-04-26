package org.r00tman.id14controlapp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val ACTION_USB_PERMISSION = "org.r00tman.id14controlapp.USB_PERMISSION"

    lateinit var usbManager: UsbManager
//    lateinit var device: UsbDevice

    lateinit var usbReceiver: BroadcastReceiver;

    fun getVolume(vol: Double): ByteArray {
        val res = (32768+32767*vol).toUInt()
        val lo = res and 0xffu
        val hi = res shr 8
        return byteArrayOf(lo.toByte(), hi.toByte())
    }

    fun onClick(view: View) {
        usbManager =  getSystemService(Context.USB_SERVICE) as UsbManager

        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.apply {

                                device?.getInterface(0)?.also { intf ->
                                    usbManager.openDevice(device)?.apply {
                                        claimInterface(intf, true)
                                        val volDouble = seekBar.progress.toDouble()/seekBar.max.toDouble()
                                        val vol = getVolume(volDouble)
                                        controlTransfer(0x21,0x1,0x1200,0x3600, vol, vol.size,0)
                                        close()
                                        Log.d("lol", "changed")
                                    }
                                }
                            }
                        } else {
                            Log.d("lol", "permission denied for device $device")
                        }
                    }
                }
            }
        }

        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)

        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        deviceList.values.forEach { device ->
            if (device.vendorId == 0x2708 && device.productId == 0x0002) {
                Log.d("lol", device.toString())
                usbManager.requestPermission(device, permissionIntent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

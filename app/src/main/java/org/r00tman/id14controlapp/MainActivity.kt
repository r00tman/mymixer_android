package org.r00tman.id14controlapp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val ACTION_USB_PERMISSION = "org.r00tman.id14controlapp.USB_PERMISSION"

    lateinit var usbManager: UsbManager

    fun getVolume(vol: Double): ByteArray {
        val res = (32768+32767*vol).toUInt()
        val lo = res and 0xffu
        val hi = res shr 8
        return byteArrayOf(lo.toByte(), hi.toByte())
    }

    fun monitorCT(dev: UsbDeviceConnection) {
        val volDouble = monitorVolume.progress.toDouble()/monitorVolume.max.toDouble()
        val one = getVolume(volDouble)
        val zero = getVolume(.0)
        dev.controlTransfer(0x21,0x1,0x0100,0x3c00, one, one.size,0)
        dev.controlTransfer(0x21,0x1,0x0101,0x3c00, zero, zero.size,0)
        dev.controlTransfer(0x21,0x1,0x0104,0x3c00, zero, zero.size,0)
        dev.controlTransfer(0x21,0x1,0x0105,0x3c00, one, one.size,0)
    }

    fun speakerCT(dev: UsbDeviceConnection) {
        val volDouble = speakerVolume.progress.toDouble()/speakerVolume.max.toDouble()
        val vol = getVolume(volDouble)
        dev.controlTransfer(0x21,0x1,0x1200,0x3600, vol, vol.size,0)
    }

    fun headphoneCT(dev: UsbDeviceConnection) {
        val volDouble = headphoneVolume.progress.toDouble()/headphoneVolume.max.toDouble()
        val vol = getVolume(volDouble)
        dev.controlTransfer(0x21,0x1,0x0203,0x0a00, vol, vol.size,0)
        dev.controlTransfer(0x21,0x1,0x0204,0x0a00, vol, vol.size,0)
    }

    fun setVolume(ct: (UsbDeviceConnection) -> Unit) {
        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.apply {
                                device?.getInterface(0)?.also { intf ->
                                    usbManager.openDevice(device)?.apply {
                                        claimInterface(intf, true)
                                        ct(this)
                                        releaseInterface(intf)
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

    fun onMClick(view: View) {
        setVolume { dev -> this.monitorCT(dev) }
    }

    fun onSPClick(view: View) {
        setVolume { dev -> this.speakerCT(dev) }
    }

    fun onHPClick(view: View) {
        setVolume { dev -> this.headphoneCT(dev) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
    }
}

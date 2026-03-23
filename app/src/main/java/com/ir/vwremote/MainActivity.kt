package com.ir.vwremote

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ir.vwremote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var irManager: ConsumerIrManager? = null

    private val commandMap = linkedMapOf(
        "Power" to 0x10EF,
        "Home" to 0xD02F,
        "Menu" to 0xC23D,
        "Source" to 0x906F,
        "Up" to 0x02FD,
        "Down" to 0x827D,
        "Left" to 0xE01F,
        "Right" to 0x609F,
        "OK" to 0x22DD,
        "Back" to 0x14EB,
        "Vol +" to 0x40BF,
        "Vol -" to 0xC03F,
        "Mute" to 0xA05F,
        "CH +" to 0x00FF,
        "CH -" to 0x807F,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        irManager = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        renderIrStatus()
        bindButtons()
    }

    private fun renderIrStatus() {
        val manager = irManager
        binding.statusText.text = when {
            manager == null -> "This device does not expose Android's Consumer IR service."
            !manager.hasIrEmitter() -> "IR service found, but no IR blaster/emitter is available on this phone."
            else -> "IR blaster detected. Point the top of the phone toward the TV and test the buttons below."
        }
    }

    private fun bindButtons() {
        binding.powerButton.setOnClickListener { sendNamedCommand("Power") }
        binding.homeButton.setOnClickListener { sendNamedCommand("Home") }
        binding.menuButton.setOnClickListener { sendNamedCommand("Menu") }
        binding.sourceButton.setOnClickListener { sendNamedCommand("Source") }
        binding.upButton.setOnClickListener { sendNamedCommand("Up") }
        binding.downButton.setOnClickListener { sendNamedCommand("Down") }
        binding.leftButton.setOnClickListener { sendNamedCommand("Left") }
        binding.rightButton.setOnClickListener { sendNamedCommand("Right") }
        binding.okButton.setOnClickListener { sendNamedCommand("OK") }
        binding.backButton.setOnClickListener { sendNamedCommand("Back") }
        binding.volUpButton.setOnClickListener { sendNamedCommand("Vol +") }
        binding.volDownButton.setOnClickListener { sendNamedCommand("Vol -") }
        binding.muteButton.setOnClickListener { sendNamedCommand("Mute") }
        binding.channelUpButton.setOnClickListener { sendNamedCommand("CH +") }
        binding.channelDownButton.setOnClickListener { sendNamedCommand("CH -") }
    }

    private fun sendNamedCommand(label: String) {
        val manager = irManager
        if (manager == null || !manager.hasIrEmitter()) {
            Toast.makeText(this, "No IR blaster available on this phone.", Toast.LENGTH_SHORT).show()
            return
        }

        val frequency = binding.frequencyInput.text?.toString()?.toIntOrNull()
        val address = binding.addressInput.text?.toString()?.trim()?.toIntOrNull(16)
        val command = commandMap[label]

        if (frequency == null || frequency <= 0) {
            binding.frequencyInput.error = "Enter a valid frequency"
            return
        }
        binding.frequencyInput.error = null

        if (address == null || address !in 0x0000..0xFFFF) {
            binding.addressInput.error = "Use a 4-digit hex address"
            return
        }
        binding.addressInput.error = null

        if (command == null) {
            Toast.makeText(this, "Command mapping missing for $label", Toast.LENGTH_SHORT).show()
            return
        }

        val frame = (address shl 16) or command
        val pattern = buildNecPattern(frame)
        manager.transmit(frequency, pattern)
        Toast.makeText(
            this,
            "Sent $label at ${frequency}Hz with address ${address.toString(16).uppercase()}",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun buildNecPattern(frame: Int): IntArray {
        val pattern = ArrayList<Int>(67)
        pattern += 9000
        pattern += 4500

        for (bit in 31 downTo 0) {
            pattern += 560
            pattern += if (((frame shr bit) and 1) == 1) 1690 else 560
        }

        pattern += 560
        return pattern.toIntArray()
    }
}

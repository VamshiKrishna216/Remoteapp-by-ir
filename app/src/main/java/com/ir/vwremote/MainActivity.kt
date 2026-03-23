package com.ir.vwremote

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.ir.vwremote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var irManager: ConsumerIrManager? = null

    private val presetLibrary = UniversalRemotePresets.presets
    private var activePreset: RemotePreset? = null
    private lateinit var commandButtons: List<MaterialButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        irManager = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        commandButtons = listOf(
            binding.buttonSlot1,
            binding.buttonSlot2,
            binding.buttonSlot3,
            binding.buttonSlot4,
            binding.buttonSlot5,
            binding.buttonSlot6,
            binding.buttonSlot7,
            binding.buttonSlot8,
            binding.buttonSlot9,
            binding.buttonSlot10,
            binding.buttonSlot11,
            binding.buttonSlot12,
            binding.buttonSlot13,
            binding.buttonSlot14,
            binding.buttonSlot15,
            binding.buttonSlot16,
        )

        setupSelectors()
        setupActions()
        renderIrStatus()
    }

    private fun setupSelectors() {
        val categories = presetLibrary.map { it.category }.distinct().sorted()
        binding.categoryDropdown.setAdapter(simpleAdapter(categories))
        binding.protocolDropdown.setAdapter(simpleAdapter(IrProtocol.entries.map { it.name }))

        if (categories.isNotEmpty()) {
            binding.categoryDropdown.setText(categories.first(), false)
            updateBrands(categories.first())
        }

        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            val category = categories[position]
            updateBrands(category)
        }

        binding.brandDropdown.setOnItemClickListener { _, _, _, _ ->
            updateProfiles(
                binding.categoryDropdown.text?.toString().orEmpty(),
                binding.brandDropdown.text?.toString().orEmpty(),
            )
        }

        binding.profileDropdown.setOnItemClickListener { _, _, _, _ ->
            syncSelectedPreset()
        }
    }

    private fun setupActions() {
        commandButtons.forEach { button ->
            button.setOnClickListener {
                val command = it.tag as? IrCommand ?: return@setOnClickListener
                sendCommand(command)
            }
        }

        binding.applyPresetButton.setOnClickListener { syncSelectedPreset(showToast = true) }
        binding.sendCustomButton.setOnClickListener { sendCustomCommand() }
    }

    private fun updateBrands(category: String) {
        val brands = presetLibrary
            .filter { it.category == category }
            .map { it.brand }
            .distinct()
            .sorted()

        binding.brandDropdown.setAdapter(simpleAdapter(brands))
        val selectedBrand = brands.firstOrNull().orEmpty()
        binding.brandDropdown.setText(selectedBrand, false)
        updateProfiles(category, selectedBrand)
    }

    private fun updateProfiles(category: String, brand: String) {
        val profiles = presetLibrary
            .filter { it.category == category && it.brand == brand }
            .sortedBy { it.model }

        binding.profileDropdown.setAdapter(simpleAdapter(profiles.map { it.model }))
        val selectedProfile = profiles.firstOrNull()?.model.orEmpty()
        binding.profileDropdown.setText(selectedProfile, false)
        syncSelectedPreset()
    }

    private fun syncSelectedPreset(showToast: Boolean = false) {
        val preset = presetLibrary.firstOrNull {
            it.category == binding.categoryDropdown.text?.toString().orEmpty() &&
                it.brand == binding.brandDropdown.text?.toString().orEmpty() &&
                it.model == binding.profileDropdown.text?.toString().orEmpty()
        } ?: return

        activePreset = preset
        binding.protocolDropdown.setText(preset.protocol.name, false)
        binding.frequencyInput.setText(preset.frequency.toString())
        binding.addressInput.setText(preset.addressHex)
        binding.subtitleText.text = preset.description
        binding.profileHintText.text = preset.hint
        binding.customCommandInput.hint = preset.protocol.commandHint
        binding.addressInputLayout.helperText = preset.protocol.addressHint
        binding.customCommandInputLayout.helperText = preset.protocol.commandHelper
        renderButtonGrid(preset)

        if (showToast) {
            Toast.makeText(this, "Loaded ${preset.brand} ${preset.model}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderButtonGrid(preset: RemotePreset) {
        commandButtons.forEachIndexed { index, button ->
            val command = preset.commands.getOrNull(index)
            if (command == null) {
                button.visibility = View.GONE
                button.tag = null
            } else {
                button.visibility = View.VISIBLE
                button.text = command.label
                button.tag = command
                button.contentDescription = "${preset.category} ${command.label}"
            }
        }
    }

    private fun renderIrStatus() {
        val manager = irManager
        binding.statusText.text = when {
            manager == null -> getString(R.string.status_no_service)
            !manager.hasIrEmitter() -> getString(R.string.status_no_emitter)
            else -> getString(R.string.status_ready)
        }
    }

    private fun sendCustomCommand() {
        val rawValue = binding.customCommandInput.text?.toString()?.trim().orEmpty()
        val protocol = selectedProtocol() ?: return
        val commandValue = rawValue.toIntOrNull(16)

        if (commandValue == null) {
            binding.customCommandInput.error = getString(R.string.error_command)
            return
        }
        binding.customCommandInput.error = null

        sendCommand(IrCommand("Custom", commandValue, isCustom = true), protocol)
    }

    private fun sendCommand(command: IrCommand, overrideProtocol: IrProtocol? = null) {
        val manager = irManager
        if (manager == null || !manager.hasIrEmitter()) {
            Toast.makeText(this, getString(R.string.error_no_ir), Toast.LENGTH_SHORT).show()
            return
        }

        val protocol = overrideProtocol ?: selectedProtocol() ?: return
        val frequency = binding.frequencyInput.text?.toString()?.toIntOrNull()
        val addressValue = binding.addressInput.text?.toString()?.trim()?.toIntOrNull(16)

        if (frequency == null || frequency !in 30000..60000) {
            binding.frequencyInput.error = getString(R.string.error_frequency)
            return
        }
        binding.frequencyInput.error = null

        if (addressValue == null || addressValue !in 0..protocol.maxAddress) {
            binding.addressInput.error = protocol.addressError
            return
        }
        binding.addressInput.error = null

        if (command.value !in 0..protocol.maxCommand) {
            binding.customCommandInput.error = protocol.commandError
            return
        }

        val pattern = protocol.buildPattern(addressValue, command.value)
        manager.transmit(frequency, pattern)

        val sourceLabel = activePreset?.let { "${it.brand} ${it.model}" } ?: protocol.name
        Toast.makeText(
            this,
            "Sent ${command.label} for $sourceLabel at ${frequency}Hz",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun selectedProtocol(): IrProtocol? {
        val name = binding.protocolDropdown.text?.toString()?.trim().orEmpty()
        val protocol = IrProtocol.entries.firstOrNull { it.name == name }
        if (protocol == null) {
            Toast.makeText(this, getString(R.string.error_protocol), Toast.LENGTH_SHORT).show()
        }
        return protocol
    }

    private fun simpleAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }
}

data class RemotePreset(
    val category: String,
    val brand: String,
    val model: String,
    val protocol: IrProtocol,
    val frequency: Int,
    val addressHex: String,
    val description: String,
    val hint: String,
    val commands: List<IrCommand>,
)

data class IrCommand(
    val label: String,
    val value: Int,
    val isCustom: Boolean = false,
)

enum class IrProtocol(
    val maxAddress: Int,
    val maxCommand: Int,
    val addressHint: String,
    val addressError: String,
    val commandHint: String,
    val commandHelper: String,
    val commandError: String,
) {
    NEC16(
        maxAddress = 0xFFFF,
        maxCommand = 0xFFFF,
        addressHint = "NEC uses a 16-bit hex device address, e.g. 20DF or E0E0.",
        addressError = "Enter a 4-digit hex device address.",
        commandHint = "Command hex, e.g. 10EF",
        commandHelper = "NEC presets use full 16-bit command words, often command + inverse.",
        commandError = "Enter a 4-digit hex NEC command.",
    ) {
        override fun buildPattern(address: Int, command: Int): IntArray {
            val frame = (address shl 16) or command
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
    },
    SIRC12(
        maxAddress = 0x1F,
        maxCommand = 0x7F,
        addressHint = "Sony SIRC12 uses a 5-bit address in hex, usually 01-1F.",
        addressError = "Enter a 1-2 digit hex address (00-1F).",
        commandHint = "Command hex, e.g. 15",
        commandHelper = "SIRC12 uses a 7-bit command, sent least-significant bit first.",
        commandError = "Enter a 1-2 digit hex Sony command.",
    ) {
        override fun buildPattern(address: Int, command: Int): IntArray {
            val frame = ((address and 0x1F) shl 7) or (command and 0x7F)
            val pattern = ArrayList<Int>(26)
            pattern += 2400
            pattern += 600
            for (bit in 0 until 12) {
                val one = ((frame shr bit) and 1) == 1
                pattern += if (one) 1200 else 600
                pattern += 600
            }
            return pattern.toIntArray()
        }
    };

    abstract fun buildPattern(address: Int, command: Int): IntArray
}

object UniversalRemotePresets {
    val presets = listOf(
        RemotePreset(
            category = "TV",
            brand = "Samsung",
            model = "Classic LCD / LED",
            protocol = IrProtocol.NEC16,
            frequency = 38000,
            addressHex = "E0E0",
            description = "Common Samsung TV layout with navigation, source, channel and volume controls.",
            hint = "If power works but some keys do not, keep the Samsung preset and test a custom NEC command.",
            commands = listOf(
                IrCommand("Power", 0x40BF),
                IrCommand("Source", 0x807F),
                IrCommand("Menu", 0x58A7),
                IrCommand("Mute", 0xF00F),
                IrCommand("Up", 0x06F9),
                IrCommand("Left", 0xA659),
                IrCommand("OK", 0x16E9),
                IrCommand("Right", 0x46B9),
                IrCommand("Down", 0x8679),
                IrCommand("Back", 0x1AE5),
                IrCommand("Home", 0x9E61),
                IrCommand("Guide", 0xF20D),
                IrCommand("Vol +", 0xE01F),
                IrCommand("Vol -", 0xD02F),
                IrCommand("CH +", 0x48B7),
                IrCommand("CH -", 0x08F7),
            ),
        ),
        RemotePreset(
            category = "TV",
            brand = "LG",
            model = "Smart TV",
            protocol = IrProtocol.NEC16,
            frequency = 38000,
            addressHex = "20DF",
            description = "LG TV preset with the common NEC address and everyday controls.",
            hint = "For many LG-compatible panels and rebadged TVs, this preset is a strong starting point.",
            commands = listOf(
                IrCommand("Power", 0x10EF),
                IrCommand("Input", 0xD02F),
                IrCommand("Home", 0xDA25),
                IrCommand("Mute", 0x906F),
                IrCommand("Up", 0x02FD),
                IrCommand("Left", 0xE01F),
                IrCommand("OK", 0x22DD),
                IrCommand("Right", 0x609F),
                IrCommand("Down", 0x827D),
                IrCommand("Back", 0x14EB),
                IrCommand("Settings", 0xC23D),
                IrCommand("Info", 0x55AA),
                IrCommand("Vol +", 0x40BF),
                IrCommand("Vol -", 0xC03F),
                IrCommand("CH +", 0x00FF),
                IrCommand("CH -", 0x807F),
            ),
        ),
        RemotePreset(
            category = "TV",
            brand = "Sony",
            model = "Bravia (SIRC12)",
            protocol = IrProtocol.SIRC12,
            frequency = 40000,
            addressHex = "01",
            description = "Sony Bravia-style preset using the 12-bit SIRC protocol.",
            hint = "Sony devices usually need SIRC rather than NEC, so keep the Sony protocol selected here.",
            commands = listOf(
                IrCommand("Power", 0x15),
                IrCommand("Input", 0x25),
                IrCommand("Home", 0x60),
                IrCommand("Mute", 0x14),
                IrCommand("Up", 0x74),
                IrCommand("Left", 0x34),
                IrCommand("OK", 0x65),
                IrCommand("Right", 0x33),
                IrCommand("Down", 0x75),
                IrCommand("Back", 0x23),
                IrCommand("Options", 0x36),
                IrCommand("Guide", 0x5B),
                IrCommand("Vol +", 0x12),
                IrCommand("Vol -", 0x13),
                IrCommand("CH +", 0x10),
                IrCommand("CH -", 0x11),
            ),
        ),
        RemotePreset(
            category = "TV",
            brand = "VW",
            model = "Linux Frameless Series",
            protocol = IrProtocol.NEC16,
            frequency = 38000,
            addressHex = "20DF",
            description = "The earlier VW preset is preserved here as one profile inside the larger universal remote app.",
            hint = "Use this if you specifically need the earlier VW32C3 / VW24C3 / VW43S1 style mapping.",
            commands = listOf(
                IrCommand("Power", 0x10EF),
                IrCommand("Home", 0xD02F),
                IrCommand("Menu", 0xC23D),
                IrCommand("Source", 0x906F),
                IrCommand("Up", 0x02FD),
                IrCommand("Left", 0xE01F),
                IrCommand("OK", 0x22DD),
                IrCommand("Right", 0x609F),
                IrCommand("Down", 0x827D),
                IrCommand("Back", 0x14EB),
                IrCommand("Mute", 0xA05F),
                IrCommand("Info", 0x55AA),
                IrCommand("Vol +", 0x40BF),
                IrCommand("Vol -", 0xC03F),
                IrCommand("CH +", 0x00FF),
                IrCommand("CH -", 0x807F),
            ),
        ),
        RemotePreset(
            category = "AC",
            brand = "Daikin",
            model = "Split AC Basic",
            protocol = IrProtocol.NEC16,
            frequency = 38000,
            addressHex = "11DA",
            description = "Compact AC control layout for power, temperature and airflow testing on IR phones.",
            hint = "Real AC remotes often send long state frames. This app uses quick test commands for basic IR experimentation.",
            commands = listOf(
                IrCommand("Power", 0x01FE),
                IrCommand("Cool", 0x02FD),
                IrCommand("Heat", 0x03FC),
                IrCommand("Dry", 0x04FB),
                IrCommand("Temp +", 0x05FA),
                IrCommand("Temp -", 0x06F9),
                IrCommand("Fan +", 0x07F8),
                IrCommand("Fan -", 0x08F7),
                IrCommand("Swing", 0x09F6),
                IrCommand("Sleep", 0x0AF5),
                IrCommand("Turbo", 0x0BF4),
                IrCommand("Timer", 0x0CF3),
            ),
        ),
        RemotePreset(
            category = "AC",
            brand = "Voltas",
            model = "Window / Split",
            protocol = IrProtocol.NEC16,
            frequency = 38000,
            addressHex = "A25D",
            description = "Voltas-oriented AC profile for the common daily controls people test first.",
            hint = "If only some controls work, keep the frequency and try nearby custom command values.",
            commands = listOf(
                IrCommand("Power", 0x12ED),
                IrCommand("Cool", 0x13EC),
                IrCommand("Heat", 0x14EB),
                IrCommand("Fan", 0x15EA),
                IrCommand("Temp +", 0x16E9),
                IrCommand("Temp -", 0x17E8),
                IrCommand("Swing", 0x18E7),
                IrCommand("Sleep", 0x19E6),
                IrCommand("Timer", 0x1AE5),
                IrCommand("Turbo", 0x1BE4),
                IrCommand("Display", 0x1CE3),
                IrCommand("Eco", 0x1DE2),
            ),
        ),
        RemotePreset(
            category = "Set-top Box",
            brand = "Tata Play",
            model = "HD Box",
            protocol = IrProtocol.NEC16,
            frequency = 38000,
            addressHex = "7F80",
            description = "Navigation-heavy preset for a TV-connected DTH or cable box.",
            hint = "Use the top row for power/menu/guide before trying numeric functions not included here.",
            commands = listOf(
                IrCommand("Power", 0x12ED),
                IrCommand("Guide", 0x13EC),
                IrCommand("Menu", 0x14EB),
                IrCommand("Mute", 0x15EA),
                IrCommand("Up", 0x16E9),
                IrCommand("Left", 0x17E8),
                IrCommand("OK", 0x18E7),
                IrCommand("Right", 0x19E6),
                IrCommand("Down", 0x1AE5),
                IrCommand("Back", 0x1BE4),
                IrCommand("Info", 0x1CE3),
                IrCommand("Record", 0x1DE2),
                IrCommand("Vol +", 0x1EE1),
                IrCommand("Vol -", 0x1FE0),
                IrCommand("CH +", 0x20DF),
                IrCommand("CH -", 0x21DE),
            ),
        ),
        RemotePreset(
            category = "Streaming Box",
            brand = "Mi",
            model = "TV Box / Stick",
            protocol = IrProtocol.NEC16,
            frequency = 38000,
            addressHex = "FD02",
            description = "Preset for IR-capable Android TV and streaming boxes that use a simple navigation remote.",
            hint = "Some streaming sticks are Bluetooth-only, so test power and navigation before assuming IR support.",
            commands = listOf(
                IrCommand("Power", 0xA25D),
                IrCommand("Home", 0xE21D),
                IrCommand("Apps", 0xD22D),
                IrCommand("Mute", 0xF00F),
                IrCommand("Up", 0x22DD),
                IrCommand("Left", 0x02FD),
                IrCommand("OK", 0xC23D),
                IrCommand("Right", 0x609F),
                IrCommand("Down", 0xA857),
                IrCommand("Back", 0x14EB),
                IrCommand("Menu", 0x906F),
                IrCommand("Voice?", 0xB04F),
            ),
        ),
        RemotePreset(
            category = "Fan",
            brand = "Ceiling Fan",
            model = "Generic IR Fan",
            protocol = IrProtocol.NEC16,
            frequency = 38000,
            addressHex = "55AA",
            description = "Generic fan profile aimed at speed, swing and timer controls.",
            hint = "Fan brands vary widely, so this profile is best used as a starting point for custom testing.",
            commands = listOf(
                IrCommand("Power", 0x10EF),
                IrCommand("Speed 1", 0x11EE),
                IrCommand("Speed 2", 0x12ED),
                IrCommand("Speed 3", 0x13EC),
                IrCommand("Speed 4", 0x14EB),
                IrCommand("Timer", 0x15EA),
                IrCommand("Swing", 0x16E9),
                IrCommand("Breeze", 0x17E8),
                IrCommand("Sleep", 0x18E7),
                IrCommand("Light", 0x19E6),
            ),
        ),
        RemotePreset(
            category = "Projector",
            brand = "Epson",
            model = "Home Projector",
            protocol = IrProtocol.NEC16,
            frequency = 38000,
            addressHex = "4BB4",
            description = "Projector profile focused on power, source, menu and directional control.",
            hint = "Projectors often need a long warm-up, so wait a few seconds after sending power.",
            commands = listOf(
                IrCommand("Power", 0x08F7),
                IrCommand("Source", 0x10EF),
                IrCommand("Menu", 0x30CF),
                IrCommand("Esc", 0x28D7),
                IrCommand("Up", 0x02FD),
                IrCommand("Left", 0xE01F),
                IrCommand("Enter", 0x22DD),
                IrCommand("Right", 0x609F),
                IrCommand("Down", 0x827D),
                IrCommand("A/V Mute", 0xC23D),
                IrCommand("Auto", 0xD02F),
                IrCommand("Freeze", 0xF00F),
            ),
        ),
        RemotePreset(
            category = "Audio",
            brand = "Sony",
            model = "Soundbar / AVR",
            protocol = IrProtocol.SIRC12,
            frequency = 40000,
            addressHex = "10",
            description = "Sony audio preset with volume, input and playback-centered commands.",
            hint = "This is useful for soundbars and some AV receivers that still expose IR control.",
            commands = listOf(
                IrCommand("Power", 0x15),
                IrCommand("Input", 0x25),
                IrCommand("Mute", 0x14),
                IrCommand("Sound", 0x3D),
                IrCommand("Vol +", 0x12),
                IrCommand("Vol -", 0x13),
                IrCommand("Play/Pause", 0x32),
                IrCommand("Prev", 0x33),
                IrCommand("Next", 0x34),
                IrCommand("Bass +", 0x5D),
                IrCommand("Bass -", 0x5E),
                IrCommand("Mode", 0x36),
            ),
        ),
    )
}

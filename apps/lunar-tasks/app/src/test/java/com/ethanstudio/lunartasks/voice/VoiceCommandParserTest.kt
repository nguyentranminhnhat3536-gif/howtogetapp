package com.ethanstudio.lunartasks.voice

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VoiceCommandParserTest {
    // Thứ Ba 29/9/2026, 10:00 sáng.
    private val now = LocalDateTime.of(2026, 9, 29, 10, 0)
    private val today = LocalDate.of(2026, 9, 29)

    private fun parse(text: String) = VoiceCommandParser.parse(text, now)

    @Test
    fun foldKeepsLength() {
        val text = "Thêm việc đi chợ lúc 8 giờ rưỡi tối"
        assertEquals(text.length, VoiceCommandParser.fold(text).length)
        assertEquals("them viec di cho luc 8 gio ruoi toi", VoiceCommandParser.fold(text))
    }

    @Test
    fun addTaskWithDateAndTime() {
        assertEquals(
            VoiceCommand.AddTask("Đi chợ", today.plusDays(1), 8 * 60),
            parse("Thêm việc đi chợ lúc 8 giờ sáng mai"),
        )
        assertEquals(
            VoiceCommand.AddTask("Đón con", today, 17 * 60 + 30),
            parse("nhắc tôi đón con lúc 5 giờ rưỡi chiều"),
        )
        assertEquals(
            VoiceCommand.AddTask("Gọi mẹ", today.plusDays(1), 20 * 60 + 15),
            parse("thêm việc gọi mẹ 8 giờ 15 tối mai"),
        )
    }

    @Test
    fun addTaskTimeAlreadyPassedMovesToTomorrow() {
        assertEquals(VoiceCommand.AddTask("Tập thể dục", today.plusDays(1), 6 * 60), parse("thêm việc tập thể dục 6h"))
        assertEquals(VoiceCommand.AddTask("Họp", today, 14 * 60), parse("thêm việc họp 14h"))
    }

    @Test
    fun addTaskDateOnlyAndNoDate() {
        assertEquals(VoiceCommand.AddTask("Mua 2 hộp sữa", today.plusDays(2), null), parse("ghi nhớ mua 2 hộp sữa ngày kia"))
        assertEquals(VoiceCommand.AddTask("Đóng tiền điện", null, null), parse("thêm việc đóng tiền điện"))
    }

    @Test
    fun addTaskEnglish() {
        assertEquals(VoiceCommand.AddTask("Buy milk", today.plusDays(1), 20 * 60), parse("remind me to buy milk tomorrow at 8 pm"))
        assertEquals(VoiceCommand.AddTask("Call the bank", today, 15 * 60 + 30), parse("add task call the bank at 3:30 pm"))
    }

    @Test
    fun addWithoutTitleOpensEditor() {
        assertEquals(VoiceCommand.OpenAddTask, parse("thêm việc"))
        assertEquals(VoiceCommand.OpenAddTask, parse("thêm việc ngày mai"))
    }

    @Test
    fun toggles() {
        assertEquals(VoiceCommand.SetHighContrast(true), parse("bật tương phản cao"))
        assertEquals(VoiceCommand.SetHighContrast(false), parse("Tắt tương phản"))
        assertEquals(VoiceCommand.SetHighContrast(null), parse("chế độ đen vàng"))
        assertEquals(VoiceCommand.SetSpeakReminders(true), parse("bật đọc to lời nhắc"))
        assertEquals(VoiceCommand.SetSpeakReminders(false), parse("tắt đọc nhắc"))
        assertEquals(VoiceCommand.SetHaptics(false), parse("tắt rung"))
        assertEquals(VoiceCommand.SetHaptics(true), parse("turn on vibration"))
    }

    @Test
    fun textSize() {
        assertEquals(VoiceCommand.ChangeTextSize(1), parse("chữ to hơn"))
        assertEquals(VoiceCommand.ChangeTextSize(-1), parse("thu nhỏ chữ"))
        assertEquals(VoiceCommand.ChangeTextSize(1), parse("bigger text"))
    }

    @Test
    fun otherCommands() {
        assertEquals(VoiceCommand.ReadToday, parse("đọc việc hôm nay"))
        assertEquals(VoiceCommand.ReadToday, parse("hôm nay có gì"))
        assertEquals(VoiceCommand.CallContact, parse("gọi người thân"))
        assertEquals(VoiceCommand.AddMedicine, parse("thêm thuốc"))
        assertEquals(VoiceCommand.AddMedicine, parse("nhắc uống thuốc"))
        assertEquals(VoiceCommand.OpenSettings, parse("mở cài đặt"))
        assertEquals(VoiceCommand.Help, parse("tôi nói gì được"))
        assertEquals(VoiceCommand.Unknown, parse("trung thu"))
        assertEquals(VoiceCommand.Unknown, parse("   "))
    }
}

package com.amaze.filemanager.ui.dialogs

import android.content.Intent
import android.os.Environment
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.MDButton
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_AESCRYPT
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_ENCRYPT_TARGET
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_OPEN_MODE
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_SOURCE
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RandomPathGenerator
import com.amaze.filemanager.filesystem.files.CryptUtil.AESCRYPT_EXTENSION
import com.amaze.filemanager.filesystem.files.CryptUtil.CRYPT_EXTENSION
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils
import com.amaze.filemanager.test.getString
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import com.amaze.filemanager.ui.views.WarnableTextInputLayout
import com.google.android.material.textfield.TextInputEditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.ShadowDialog
import java.io.File
import kotlin.random.Random

/**
 * Logic test for [EncryptAuthenticateDialog].
 */
class EncryptAuthenticateDialogTest : AbstractEncryptDialogTests() {

    private val randomizer = Random(System.currentTimeMillis())
    private lateinit var file: File
    private lateinit var tilFileSaveAs: WarnableTextInputLayout
    private lateinit var tilEncryptPassword: WarnableTextInputLayout
    private lateinit var tilEncryptPasswordConfirm: WarnableTextInputLayout
    private lateinit var editTextFileSaveAs: TextInputEditText
    private lateinit var editTextEncryptPassword: TextInputEditText
    private lateinit var editTextEncryptPasswordConfirm: TextInputEditText
    private lateinit var checkboxUseAze: AppCompatCheckBox
    private lateinit var okButton: MDButton

    /**
     * MainActivity setup.
     */
    @Before
    override fun setUp() {
        super.setUp()
        file = File(
            Environment.getExternalStorageDirectory(),
            RandomPathGenerator.generateRandomPath(
                randomizer, 16
            )
        )
    }

    /**
     * Test simple workflow. Just enter a password and off it go.
     */
    @Test
    @Suppress("StringLiteralDuplication")
    fun testSimpleWorkflow() {
        performTest(
            { _, _, _ ->
                assertEquals("${file.name}$AESCRYPT_EXTENSION", editTextFileSaveAs.text.toString())
                editTextEncryptPassword.setText("abcdefgh")
                editTextEncryptPasswordConfirm.setText("abcdefgh")
                assertTrue(okButton.isEnabled)
                okButton.performClick()
            },
            object : EncryptDecryptUtils.EncryptButtonCallbackInterface {
                override fun onButtonPressed(intent: Intent, password: String) {
                    assertEquals(file.absolutePath, intent.getStringExtra(TAG_SOURCE))
                    assertTrue(intent.getBooleanExtra(TAG_AESCRYPT, false))
                    assertEquals(
                        "${file.name}$AESCRYPT_EXTENSION",
                        intent.getStringExtra(TAG_ENCRYPT_TARGET)
                    )
                    assertEquals("abcdefgh", password)
                }
            }
        )
    }

    /**
     * Test simple workflow with aze encryption checkbox checked. Also renamed target filename
     */
    @Test
    fun testAzeWorkflow() {
        performTest(
            { _, _, _ ->
                assertEquals("${file.name}$AESCRYPT_EXTENSION", editTextFileSaveAs.text.toString())
                editTextEncryptPassword.setText("abcdefgh")
                editTextEncryptPasswordConfirm.setText("abcdefgh")
                assertTrue(okButton.isEnabled)
                checkboxUseAze.isChecked = true
                assertEquals("${file.name}$CRYPT_EXTENSION", editTextFileSaveAs.text.toString())
                editTextFileSaveAs.setText("testfile$CRYPT_EXTENSION")
                okButton.performClick()
            },
            object : EncryptDecryptUtils.EncryptButtonCallbackInterface {
                override fun onButtonPressed(intent: Intent, password: String) {
                    assertEquals(file.absolutePath, intent.getStringExtra(TAG_SOURCE))
                    assertFalse(intent.getBooleanExtra(TAG_AESCRYPT, true))
                    assertEquals(
                        "testfile$CRYPT_EXTENSION",
                        intent.getStringExtra(TAG_ENCRYPT_TARGET)
                    )
                    assertEquals("abcdefgh", password)
                }
            }
        )
    }

    /**
     * Test password field validation.
     */
    @Test
    fun testPasswordValidations() {
        performTest({ _, _, _ ->
            editTextEncryptPassword.setText("abc")
            editTextEncryptPasswordConfirm.setText("def")
            assertFalse(okButton.isEnabled)
            assertEquals(
                getString(R.string.password_no_match),
                tilEncryptPasswordConfirm.error
            )
            editTextEncryptPassword.setText("")
            editTextEncryptPasswordConfirm.setText("")
            assertFalse(okButton.isEnabled)
            assertEquals(getString(R.string.field_empty), tilEncryptPassword.error)
            editTextEncryptPassword.setText("abcdef")
            editTextEncryptPasswordConfirm.setText("abcdef")
            assertTrue(okButton.isEnabled)
        })
    }

    /**
     * Test filename field validations.
     */
    @Test
    fun testFilenameValidations() {
        performTest({ _, _, _ ->
            editTextFileSaveAs.setText("${file.name}.error")
            assertFalse(okButton.isEnabled)
            assertEquals(
                getString(R.string.encrypt_file_must_end_with_aes),
                tilFileSaveAs.error
            )
            editTextFileSaveAs.setText("${file.name}.aze")
            assertFalse(okButton.isEnabled)
            assertEquals(
                getString(R.string.encrypt_file_must_end_with_aes),
                tilFileSaveAs.error
            )
            checkboxUseAze.isChecked = true
            assertTrue(okButton.isEnabled)
            assertNull(tilFileSaveAs.error)
            editTextFileSaveAs.setText("${file.name}.error")
            assertFalse(okButton.isEnabled)
            assertEquals(
                getString(R.string.encrypt_file_must_end_with_aze),
                tilFileSaveAs.error
            )
            editTextFileSaveAs.setText("${file.name}.aes")
            assertFalse(okButton.isEnabled)
            assertEquals(
                getString(R.string.encrypt_file_must_end_with_aze),
                tilFileSaveAs.error
            )
            editTextFileSaveAs.setText("${file.name}.aze")
            assertTrue(okButton.isEnabled)
            assertNull(tilFileSaveAs.error)
        })
    }

    /**
     * To test how would Amaze legacy encryption dialog pops up.
     */
    @Test
    fun testAzeCryptDialog() {
        performTest({ _, _, _ ->
            checkboxUseAze.isChecked = true
            assertTrue(ShadowDialog.getShownDialogs().size == 2)
            assertTrue(ShadowDialog.getLatestDialog() is MaterialDialog)
            (ShadowDialog.getLatestDialog() as MaterialDialog).run {
                assertEquals(getString(R.string.warning), titleView.text)
                assertEquals(getString(R.string.crypt_warning_key), contentView?.text.toString())
                assertEquals(
                    getString(R.string.warning_never_show),
                    getActionButton(DialogAction.NEGATIVE).text
                )
                assertEquals(
                    getString(R.string.warning_confirm),
                    getActionButton(DialogAction.POSITIVE).text
                )
                assertTrue(getActionButton(DialogAction.POSITIVE).performClick())
            }
            assertEquals(2, ShadowDialog.getShownDialogs().size)
            assertFalse(ShadowDialog.getLatestDialog().isShowing)
            assertTrue(true == editTextFileSaveAs.text?.endsWith(CRYPT_EXTENSION))
            checkboxUseAze.isChecked = false
            assertEquals(2, ShadowDialog.getShownDialogs().size)
            assertFalse(ShadowDialog.getLatestDialog().isShowing)
            assertTrue(true == editTextFileSaveAs.text?.endsWith(AESCRYPT_EXTENSION))
            checkboxUseAze.isChecked = true
            assertEquals(3, ShadowDialog.getShownDialogs().size)
            assertTrue(ShadowDialog.getLatestDialog().isShowing)
            assertTrue(true == editTextFileSaveAs.text?.endsWith(CRYPT_EXTENSION))
            (ShadowDialog.getLatestDialog() as MaterialDialog)
                .getActionButton(DialogAction.NEGATIVE).performClick()
            assertEquals(3, ShadowDialog.getShownDialogs().size)
            assertFalse(ShadowDialog.getLatestDialog().isShowing)
            assertTrue(
                PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
                    .getBoolean(PreferencesConstants.PREFERENCE_CRYPT_WARNING_REMEMBER, false)
            )
            checkboxUseAze.isChecked = false
            assertEquals(3, ShadowDialog.getShownDialogs().size) // no new dialog
            checkboxUseAze.isChecked = true
            assertEquals(3, ShadowDialog.getShownDialogs().size)
        })
    }

    /**
     * prepare MainActivity scenario. Also populate dialog fields to test
     *
     * @param testContent main test function
     * @param callback optional callback for result verification
     */
    private fun performTest(
        testContent: (
            dialog: MaterialDialog,
            intent: Intent,
            activity: MainActivity
        ) -> Unit,
        callback: EncryptDecryptUtils.EncryptButtonCallbackInterface =
            object : EncryptDecryptUtils.EncryptButtonCallbackInterface {}
    ) {
        scenario.onActivity { activity ->
            Intent().putExtra(TAG_SOURCE, HybridFileParcelable(file.absolutePath))
                .putExtra(TAG_OPEN_MODE, OpenMode.FILE).let { intent ->
                    EncryptAuthenticateDialog.show(
                        activity,
                        intent,
                        activity,
                        activity.appTheme,
                        callback
                    )
                    ShadowDialog.getLatestDialog()?.run {
                        assertTrue(this is MaterialDialog)
                        (this as MaterialDialog).let {
                            editTextFileSaveAs = findViewById<TextInputEditText>(
                                R.id.edit_text_encrypt_save_as
                            )
                            editTextEncryptPassword = findViewById<TextInputEditText>(
                                R.id.edit_text_dialog_encrypt_password
                            )
                            editTextEncryptPasswordConfirm = findViewById<TextInputEditText>(
                                R.id.edit_text_dialog_encrypt_password_confirm
                            )
                            tilFileSaveAs = findViewById<WarnableTextInputLayout>(
                                R.id.til_encrypt_save_as
                            )
                            tilEncryptPassword = findViewById<WarnableTextInputLayout>(
                                R.id.til_encrypt_password
                            )
                            tilEncryptPasswordConfirm = findViewById<WarnableTextInputLayout>(
                                R.id.til_encrypt_password
                            )
                            checkboxUseAze = findViewById<AppCompatCheckBox>(R.id.checkbox_use_aze)
                            okButton = getActionButton(DialogAction.POSITIVE)
                            assertFalse(okButton.isEnabled)
                            assertTrue(true == editTextFileSaveAs.text?.startsWith(file.name))
                            testContent.invoke(it, intent, activity)
                        }
                    } ?: fail("Dialog cannot be seen?")
                }
        }
    }
}

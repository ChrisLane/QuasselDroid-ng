package de.kuschku.quasseldroid.ui.setup.accounts.setup

import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import butterknife.BindView
import butterknife.ButterKnife
import de.kuschku.quasseldroid.R
import de.kuschku.quasseldroid.ui.setup.SlideFragment
import de.kuschku.quasseldroid.util.TextValidator

class AccountSetupUserSlide : SlideFragment() {
  @BindView(R.id.userWrapper)
  lateinit var userWrapper: TextInputLayout
  @BindView(R.id.user)
  lateinit var userField: EditText

  @BindView(R.id.passWrapper)
  lateinit var passWrapper: TextInputLayout
  @BindView(R.id.pass)
  lateinit var passField: EditText

  override fun isValid(): Boolean {
    return true
  }

  override val title = R.string.slide_account_user_title
  override val description = R.string.slide_account_user_description

  override fun setData(data: Bundle) {
    if (data.containsKey("user"))
      userField.setText(data.getString("user"))
    if (data.containsKey("pass"))
      passField.setText(data.getString("pass"))
  }

  override fun getData(data: Bundle) {
    data.putString("user", userField.text.toString())
    data.putString("pass", passField.text.toString())
  }

  override fun onCreateContent(inflater: LayoutInflater, container: ViewGroup?,
                               savedInstanceState: Bundle?): View {
    val view = inflater.inflate(R.layout.setup_account_user, container, false)
    ButterKnife.bind(this, view)
    userValidator = object : TextValidator(
      userWrapper::setError, resources.getString(R.string.hint_invalid_user)
    ) {
      override fun validate(text: Editable) = text.isNotBlank()

      override fun onChanged() = updateValidity()
    }
    userField.addTextChangedListener(userValidator)
    userValidator.afterTextChanged(userField.text)
    return view
  }

  private lateinit var userValidator: TextValidator
}

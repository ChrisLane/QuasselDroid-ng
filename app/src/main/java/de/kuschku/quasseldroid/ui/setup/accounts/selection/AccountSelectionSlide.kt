package de.kuschku.quasseldroid.ui.setup.accounts.selection

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import de.kuschku.quasseldroid.R
import de.kuschku.quasseldroid.persistence.AccountDatabase
import de.kuschku.quasseldroid.ui.setup.SlideFragment
import de.kuschku.quasseldroid.ui.setup.accounts.edit.AccountEditActivity
import de.kuschku.quasseldroid.ui.setup.accounts.selection.AccountSelectionActivity.Companion.REQUEST_CREATE_FIRST
import de.kuschku.quasseldroid.ui.setup.accounts.selection.AccountSelectionActivity.Companion.REQUEST_CREATE_NEW
import de.kuschku.quasseldroid.ui.setup.accounts.setup.AccountSetupActivity

class AccountSelectionSlide : SlideFragment() {
  @BindView(R.id.account_list)
  lateinit var accountList: RecyclerView

  override fun isValid() = adapter?.selectedItemId ?: -1L != -1L

  override val title = R.string.slide_account_select_title
  override val description = R.string.slide_account_select_description

  override fun setData(data: Bundle) {
    if (data.containsKey("selectedAccount"))
      adapter?.selectAccount(data.getLong("selectedAccount"))
  }

  override fun getData(data: Bundle) {
    data.putLong("selectedAccount", adapter?.selectedItemId ?: -1L)
  }

  private var adapter: AccountAdapter? = null
  override fun onCreateContent(inflater: LayoutInflater, container: ViewGroup?,
                               savedInstanceState: Bundle?): View {
    val view = inflater.inflate(R.layout.setup_select_account, container, false)
    ButterKnife.bind(this, view)
    val accountViewModel = ViewModelProviders.of(this).get(AccountViewModel::class.java)
    val firstObserver = object : Observer<List<AccountDatabase.Account>?> {
      override fun onChanged(t: List<AccountDatabase.Account>?) {
        if (t?.isEmpty() != false)
          startActivityForResult(
            Intent(context, AccountSetupActivity::class.java),
            REQUEST_CREATE_FIRST
          )
        accountViewModel.accounts.removeObserver(this)
      }
    }
    accountViewModel.accounts.observe(this, firstObserver)
    accountList.layoutManager = LinearLayoutManager(context)
    accountList.itemAnimator = DefaultItemAnimator()
    val adapter = AccountAdapter(
      this, accountViewModel.accounts, accountViewModel.selectedItem
    )
    this.adapter = adapter
    accountList.adapter = adapter

    adapter.addAddListener {
      startActivityForResult(Intent(context, AccountSetupActivity::class.java), -1)
    }
    adapter.addEditListener { id ->
      val intent = Intent(context, AccountEditActivity::class.java)
      intent.putExtra("account", id)
      startActivityForResult(intent, REQUEST_CREATE_NEW)
    }
    adapter.addSelectionListener {
      updateValidity()
    }

    return view
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CREATE_FIRST && resultCode == Activity.RESULT_CANCELED) {
      activity?.finish()
    }
  }
}

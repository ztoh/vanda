package com.vanda.vandalibnetwork.fragmentactivity;

import java.util.ArrayList;
import java.util.List;

import android.text.format.DateUtils;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.vanda.vandalibnetwork.arrayadapter.PullLoadArrayAdaper;
import com.vanda.vandalibnetwork.daterequest.GsonRequest;
import com.vanda.vandalibnetwork.daterequest.RequestManager;
import com.vanda.vandalibnetwork.utils.Pagination;

/**
 * @author vanda 伍中联 这个是为了简便Fragment包含listiew要刷新的情况，需要注意几点 1.
 *         子类需要实例化mPullToRefreshListView，mPullLoadArrayAdaper，mArrayList，mList
 *         确保数据对象被创建 2. mArrayList 只是作为数据的子载体，而mList是一个数据集合，子载体会被添加到数据集合中，
 *         所以在创建Arraydapter时，需要向其将mList参数传入。
 * @param <T>
 *            数据解析类
 * @param <K>
 *            ListView 的条目数据集合中载体
 */

public abstract class BaseListViewFragmentActivity<T, K> extends
		BaseFragmentActivity<T> implements OnRefreshListener2<ListView> {
	public PullToRefreshListView mPullToRefreshListView;
	public PullLoadArrayAdaper<K> mPullLoadArrayAdaper;
	public ArrayList<K> mArrayList;
	public List<K> mList;
	public Pagination mPage = new Pagination(20); // use for pagination
	public int mDataItemCount = 0;

	/**
	 * 这个方法必须处理父类的mArrayList和mDataItemCount
	 */
	protected abstract void addArrayListData(T response);

	@Override
	protected void onStart() {
		mPullToRefreshListView
				.setOnRefreshListener(BaseListViewFragmentActivity.this);
		super.onStart();
	}

	@Override
	protected void processData(T response) {
		super.processData(response);
		mArrayList.clear();
		mPullLoadArrayAdaper.clear();
		addArrayListData(response);
		mPage = new Pagination(mArrayList.size(), mDataItemCount, 20);
		setRefreshMode(mPage);
		mPullLoadArrayAdaper.addAll(mArrayList);
		mPullLoadArrayAdaper.notifyDataSetChanged();
	}

	protected void setRefreshMode(Pagination page) {
		mPullToRefreshListView.setMode(page.hasMore() ? Mode.BOTH
				: Mode.PULL_FROM_START);
	}

	protected abstract String getRefDataUrl(int page, int size);

	protected void loadData(final boolean nextPage) {
		String url = getRefDataUrl(mPage.page + (nextPage ? 1 : 0), mPage.size);
		if (nextPage) {
			mPage.page += 1;
		}
		Request<T> mLoadRequestData = new GsonRequest<T>(Method.GET, url,
				getResponseDataClass(), null, new Response.Listener<T>() {

					@Override
					public void onResponse(T response) {
						RequestManager
								.cancelAll(BaseListViewFragmentActivity.this);

						if (!mPullLoadArrayAdaper.isPullLoad()) {
							mPullLoadArrayAdaper.clear();
							mArrayList.clear();
						}
						mPullToRefreshListView.onRefreshComplete();
						if (response == null) {
							mArrayList.clear();
						} else {
							mArrayList.clear();
							addArrayListData(response);
							if (!nextPage) {
								mPage = new Pagination(mArrayList.size(),
										mDataItemCount, 20);
								setRefreshMode(mPage);
							} else {
								mPage.updateLoaded(mArrayList.size());
								setRefreshMode(mPage);
							}
						}
						mPullLoadArrayAdaper.addAll(mArrayList);
						mPullLoadArrayAdaper.notifyDataSetChanged();
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						RequestManager
								.cancelAll(BaseListViewFragmentActivity.this);
						mPullToRefreshListView.onRefreshComplete();
						Toast.makeText(BaseListViewFragmentActivity.this,
								"刷新失败！", Toast.LENGTH_SHORT).show();
					}
				});
		RequestManager.addRequest(mLoadRequestData,
				BaseListViewFragmentActivity.this);
	}

	@Override
	public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
		mPullLoadArrayAdaper.setPullLoad(false);
		mPage.clear();
		String label = DateUtils.formatDateTime(
				BaseListViewFragmentActivity.this, System.currentTimeMillis(),
				DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE
						| DateUtils.FORMAT_ABBREV_ALL);
		refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
		loadData(false);
	}

	@Override
	public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
		mPullLoadArrayAdaper.setPullLoad(true);
		loadData(true);
	}
}

package org.xiangbalao.cleancache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.xiangbalao.cachecleaner.R;

import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gamedashi.swipebacklayout.lib.SwipeBackLayout;
import com.gamedashi.swipebacklayout.lib.app.SwipeBackActivity;

/**
 * 清理系统缓存
 * 
 * @author longtaoge
 * 
 */
public class CleanCacheActivity extends SwipeBackActivity {
	// 滑动退出
	private static final int VIBRATE_DURATION = 0;
	private SwipeBackLayout mSwipeBackLayout;
	int total = 0;
	private TextView tv_status;
	private ProgressBar pb;
	private LinearLayout ll_container;

	private Button clean_All;

	private ListView mListView;

	private List<CacheInfo> cacheInfos;
	public CacheAdapter mCacheAdapter;

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			final CacheInfo info = (CacheInfo) msg.obj;

			cacheInfos.add(info);
			mCacheAdapter.notifyDataSetChanged();

			// mListView.setAdapter(mCacheAdapter);

		};

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.org_xiangbalao_cleancache_activity_clean_cache);

		mCacheAdapter = new CacheAdapter();

		cacheInfos = new ArrayList<CacheInfo>();
		
		findview();

		scanCache();

		// Activity 切换效果
		overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);

		mSwipeBackLayout = getSwipeBackLayout();
		mSwipeBackLayout.setEdgeTrackingEnabled(SwipeBackLayout.EDGE_LEFT);
		mSwipeBackLayout.addSwipeListener(new SwipeBackLayout.SwipeListener() {
			@Override
			public void onScrollStateChange(int state, float scrollPercent) {

			}

			@Override
			public void onEdgeTouch(int edgeFlag) {
				vibrate(VIBRATE_DURATION);
			}

			@Override
			public void onScrollOverThreshold() {
				vibrate(VIBRATE_DURATION);
			}
		});

	}

	private void findview() {
		ll_container = (LinearLayout) findViewById(R.id.org_xiangbalao_cleancache_CleanCacheActivity_container);
		pb = (ProgressBar) findViewById(R.id.org_xiangbalao_cleancache_CleanCacheActivity_container_pb);
		tv_status = (TextView) findViewById(R.id.org_xiangbalao_cleancache_CleanCacheActivity_container_tv_scan_status);
		clean_All = (Button) findViewById(R.id.org_xiangbalao_cleancache_CleanCacheActivity_cleanall);
		clean_All.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (cacheInfos.size()>= 1) {
					cleanAll();
				}else {
					Toast.makeText(CleanCacheActivity.this, "没有发现缓存文件", 1).show();
				}

			}
		});

		mListView = (ListView) findViewById(R.id.org_xiangbalao_cleancache_CleanCacheActivity_list);
		mListView.setAdapter(mCacheAdapter);

		// 条目点击事件
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// 清理单个
				Intent intent = new Intent();
				intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
				intent.addCategory("android.intent.category.DEFAULT");
				intent.setData(Uri.parse("package:"
						+ cacheInfos.get(position).packname));
				startActivity(intent);

			}
		});

	}

	/**
	 * 扫描缓存的方法
	 */
	private void scanCache() {
		new Thread() {
			public void run() {
				PackageManager pm = getPackageManager();
				List<PackageInfo> packInfos = pm.getInstalledPackages(0);
				pb.setMax(packInfos.size());
				total = 0;
				for (PackageInfo packinfo : packInfos) {
					String packname = packinfo.packageName;
					try {
						Method method = PackageManager.class.getMethod(
								"getPackageSizeInfo", String.class,
								IPackageStatsObserver.class);
						method.invoke(pm, packname, new MyObserver());
						// Log.i("cache",packname.toString());
					} catch (Exception e) {
						e.printStackTrace();
					}
					final String appname = packinfo.applicationInfo.loadLabel(
							pm).toString();
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							tv_status.setText("正在扫描：" + appname);
						}
					});
					total++;
					pb.setProgress(total);
					try {
						Thread.sleep(30);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tv_status.setText("扫描完毕！");
					}
				});
			};
		}.start();
	}

	private class MyObserver extends IPackageStatsObserver.Stub {

		@Override
		public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
				throws RemoteException {
			long cache = pStats.cacheSize;
			// 如果缓存数大于0
			if (cache > 0) {
				try {
					CacheInfo cacheInfo = new CacheInfo();
					PackageManager pm = getPackageManager();
					// 添加缓存信息到ui界面
					cacheInfo.packname = pStats.packageName;

					PackageInfo packInfo = pm.getPackageInfo(
							cacheInfo.packname, 0);
					cacheInfo.cachesize = cache;
					cacheInfo.appname = packInfo.applicationInfo.loadLabel(pm)
							.toString();
					cacheInfo.icon = packInfo.applicationInfo.loadIcon(pm);

					cacheInfo.datasise = pStats.dataSize;
					cacheInfo.cachesize = pStats.cacheSize;
					cacheInfo.codesize = pStats.codeSize;

					CacheInfo temp_cache = cacheInfo;

					Message msg = Message.obtain();

					// Log.i("cache", msg.toString());

					// Log.i("log1",
					// packInfo.applicationInfo.loadLabel(pm).toString());
					msg.obj = temp_cache;

					handler.sendMessage(msg);
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public class CacheInfo {

		public String packname;
		public String appname;
		public Drawable icon;
		public long cachesize;
		public long datasise;
		public long codesize;
	}

	public void cleanAll() {
		PackageManager pm = getPackageManager();
		Method[] methods = PackageManager.class.getMethods();
		for (Method method : methods) {
			if ("freeStorageAndNotify".equals(method.getName())) {
				try {
					method.invoke(pm,  Long.MAX_VALUE, //Integer.MAX_VALUE,
							new IPackageDataObserver.Stub() {
								@Override
								public void onRemoveCompleted(
										String packageName, boolean succeeded)
										throws RemoteException {
										if (succeeded) {
											
											
											cacheInfos.clear();
											mCacheAdapter.notifyDataSetChanged();
											Toast.makeText(CleanCacheActivity.this, "已经清理完毕",1).show();
										}
									System.out.println(succeeded);

								}
							});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
			
		}
		cacheInfos.clear();
		mCacheAdapter.notifyDataSetChanged();
		Toast.makeText(CleanCacheActivity.this, "已经清理完毕",1).show();
		//Toast.makeText(this, "清理完毕", 1).show();
	}

	private void vibrate(long duration) {
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		long[] pattern = { 0, duration };
		vibrator.vibrate(pattern, -1);
	}

	private class CacheAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return cacheInfos.size();
		}

		@Override
		public Object getItem(int position) {
			return cacheInfos.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			ViewHolder holder;
			CacheInfo info = cacheInfos.get(position);
			if (convertView == null) {
				view = View.inflate(CleanCacheActivity.this,
						R.layout.org_xiangbalao_cleancache_cache_clear_item,
						null);
				holder = new ViewHolder();
				holder.iv_icon = (ImageView) view
						.findViewById(R.id.iv_cache_icon);
				holder.tv_name = (TextView) view
						.findViewById(R.id.tv_cache_name);
				holder.tv_code = (TextView) view
						.findViewById(R.id.tv_cache_code);
				holder.tv_data = (TextView) view
						.findViewById(R.id.tv_cache_data);
				holder.tv_cache = (TextView) view
						.findViewById(R.id.tv_cache_cache);
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}
			holder.iv_icon.setImageDrawable(info.icon);
			holder.tv_name.setText(info.appname);

			holder.tv_code.setText("应用大小："
					+ String.valueOf(info.codesize / 1024) + "kb");

			holder.tv_data.setText("数据大小："
					+ String.valueOf(info.datasise / 1024) + "kb");

			holder.tv_cache.setText("缓存大小："
					+ String.valueOf(info.cachesize / 1024) + "kb");

			return view;
		}

	}

	private class ViewHolder {
		ImageView iv_icon;
		TextView tv_name;
		TextView tv_cache;
		TextView tv_code;
		TextView tv_data;

	}

}

package br.com.adley.myseriesproject.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;

import br.com.adley.myseriesproject.R;
import br.com.adley.myseriesproject.adapters.recyclerview.PopularShowsRecyclerViewAdapter;
import br.com.adley.myseriesproject.library.AppConsts;
import br.com.adley.myseriesproject.library.RecyclerItemClickListener;
import br.com.adley.myseriesproject.library.Utils;
import br.com.adley.myseriesproject.library.enums.DownloadStatus;
import br.com.adley.myseriesproject.models.TVShow;
import br.com.adley.myseriesproject.service.GetPopularShowsJsonData;

public class PopularTVShowActivity extends BaseActivity {
    private int mPage = 1;
    private int mTotalPages;
    private boolean mIsLoadMore = true;
    private int pastVisiblesItems, visibleItemCount, totalItemCount;
    private List<TVShow> mTVShowList;
    private RecyclerView mRecyclerView;
    private PopularShowsRecyclerViewAdapter mPopularShowsRecyclerViewAdapter;
    private GridLayoutManager mLayoutManager;
    private boolean mIsTablet = false;
    private ProgressBar mLoadMoreItensLayout;
    private View mProgressBarHomeLayout;
    private View mNoInternetConnection;
    private ImageView mRefreshButtonNoConnection;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_tvshow);
        activateToolbarWithHomeEnabled();

        //Ad Config
        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, getString(R.string.application_id_ad));

        // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
        // values/strings.xml.
        mAdView = (AdView) findViewById(R.id.ad_view_popular_shows);

        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(getString(R.string.device_id_test1))
                .build();

        // Start loading the ad in the background.
        mAdView.setVisibility(View.VISIBLE);
        mAdView.loadAd(adRequest);

        mTVShowList = new ArrayList<>();
        mNoInternetConnection = findViewById(R.id.no_internet_connection_layout);
        mRefreshButtonNoConnection = (ImageView) findViewById(R.id.refresh_button_no_internet);
        mLoadMoreItensLayout = (ProgressBar) findViewById(R.id.load_more_air_today_progressbar);
        mProgressBarHomeLayout = findViewById(R.id.loading_progress_popular_show_layout);
        Utils.setLayoutVisible(mProgressBarHomeLayout);

        // Create and generate the recycler view for list of results
        mPopularShowsRecyclerViewAdapter = new PopularShowsRecyclerViewAdapter(PopularTVShowActivity.this, new ArrayList<TVShow>());
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_popular_show_list);
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(mPopularShowsRecyclerViewAdapter);
        }

        mIsTablet = Utils.isTablet(this);
        if (mIsTablet) {
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                mLayoutManager = new GridLayoutManager(PopularTVShowActivity.this, AppConsts.AIRTODAY_PORTRAIT_TABLET);
            } else {
                mLayoutManager = new GridLayoutManager(PopularTVShowActivity.this, AppConsts.AIRTODAY_LANDSCAPE_TABLET);
            }
        } else {
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                mLayoutManager = new GridLayoutManager(PopularTVShowActivity.this, AppConsts.AIRTODAY_PORTRAIT_PHONE);
            } else {
                mLayoutManager = new GridLayoutManager(PopularTVShowActivity.this, AppConsts.AIRTODAY_LANDSCAPE_PHONE);
            }
        }
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) //check for scroll down
                {
                    visibleItemCount = mLayoutManager.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();

                    if (mIsLoadMore) {
                        if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                            mIsLoadMore = false;
                            mPage++;
                            if (mPage <= mTotalPages) {
                                Utils.setLayoutVisible(mLoadMoreItensLayout);
                                executePopularShowList();
                            }
                        }
                    }
                }
            }
        });
        mRefreshButtonNoConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePopularShowList();
            }
        });
        bindRecyclerView();
        executePopularShowList();
    }

    public void executePopularShowList() {
        if (Utils.checkAppConnectionStatus(PopularTVShowActivity.this)) {
            Utils.setLayoutInvisible(mNoInternetConnection);
            loadConfigPreferences(PopularTVShowActivity.this);
            String posterSize = getPosterSize();
            String backdropSize = getBackDropSize();
            boolean isLanguageUsePtBr = isLanguageUsePtBr();
            // Set Layout Visible
            Utils.setLayoutVisible(mRecyclerView);
            if (mPage < mTotalPages) mIsLoadMore = true;
            ProcessPopularTVShow processTVShowsAiringToday = new ProcessPopularTVShow(PopularTVShowActivity.this, isLanguageUsePtBr, posterSize, backdropSize, mPage);
            processTVShowsAiringToday.execute();
        } else {
            Utils.setLayoutInvisible(mRecyclerView);
            Utils.setLayoutInvisible(mProgressBarHomeLayout);
            Utils.setLayoutVisible(mNoInternetConnection);
            Utils.setLayoutInvisible(mProgressBarHomeLayout);
            Snackbar snackbarNoInternet = Snackbar
                    .make(mNoInternetConnection, this.getString(R.string.cant_load_popular_shows), Snackbar.LENGTH_LONG)
                    .setAction(this.getString(R.string.retry_snackbar), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            executePopularShowList();
                        }
                    });
            snackbarNoInternet.setActionTextColor(Color.RED);
            snackbarNoInternet.show();
        }
    }

    // Process and execute data into recycler view
    public class ProcessPopularTVShow extends GetPopularShowsJsonData {

        public ProcessPopularTVShow(Context context, boolean isLanguageUsePtBr, String posterSize, String backDropSize, int page) {
            super(context, isLanguageUsePtBr, posterSize, backDropSize, page);
        }

        public void execute() {
            Utils.setLayoutVisible(mProgressBarHomeLayout);
            ProcessData processData = new ProcessData();
            processData.execute();
        }

        public class ProcessData extends DownloadJsonData {
            protected void onPostExecute(String webData) {
                super.onPostExecute(webData);
                Utils.setLayoutInvisible(mProgressBarHomeLayout);
                if (getDownloadStatus() != DownloadStatus.OK || getTVShows() == null) {
                    Utils.setLayoutInvisible(mRecyclerView);
                    Utils.setLayoutVisible(mNoInternetConnection);
                    Snackbar snackbarNoInternet = Snackbar
                            .make(mNoInternetConnection, PopularTVShowActivity.this.getString(R.string.cant_load_popular_shows), Snackbar.LENGTH_LONG)
                            .setAction(PopularTVShowActivity.this.getString(R.string.retry_snackbar), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    executePopularShowList();
                                }
                            });
                    snackbarNoInternet.setActionTextColor(Color.RED);
                    snackbarNoInternet.show();
                } else {
                    // Set TVShow List to get when update.
                    mTotalPages = getTotalPages();
                    mPage = getPage();
                    Log.v("PAGE", String.valueOf(mPage));
                    if (mTVShowList.isEmpty() || mTVShowList == null) {
                        mTVShowList = getTVShows();
                    } else {
                        mTVShowList.addAll(getTVShows());
                    }
                    mPopularShowsRecyclerViewAdapter.loadNewData(mTVShowList);
                }
                Utils.setLayoutInvisible(mLoadMoreItensLayout);
            }
        }
    }

    // Create the touch for the recyclerview list
    public void bindRecyclerView() {
        if (mRecyclerView != null) {
            mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(PopularTVShowActivity.this, mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    //Creates and configure intent to call tv show details activity
                    Intent intent = new Intent(PopularTVShowActivity.this, DetailsTVShowActivity.class);
                    intent.putExtra(AppConsts.TVSHOW_TRANSFER, mPopularShowsRecyclerViewAdapter.getTVShow(position));
                    startActivity(intent);
                }

                @Override
                public void onItemLongClick(View view, int position) {
                    //Creates and configure intent to call tv show details activity
                    Intent intent = new Intent(PopularTVShowActivity.this, DetailsTVShowActivity.class);
                    intent.putExtra(AppConsts.TVSHOW_TRANSFER, mPopularShowsRecyclerViewAdapter.getTVShow(position));
                    startActivity(intent);
                }
            }
            ));
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mRecyclerView != null) {
            if (mIsTablet) {
                if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    mLayoutManager = new GridLayoutManager(PopularTVShowActivity.this, AppConsts.AIRTODAY_PORTRAIT_TABLET);
                } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mLayoutManager = new GridLayoutManager(PopularTVShowActivity.this, AppConsts.AIRTODAY_LANDSCAPE_TABLET);
                }
            } else {
                if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    mLayoutManager = new GridLayoutManager(PopularTVShowActivity.this, AppConsts.AIRTODAY_PORTRAIT_PHONE);
                } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mLayoutManager = new GridLayoutManager(PopularTVShowActivity.this, AppConsts.AIRTODAY_LANDSCAPE_PHONE);
                }
            }
            mRecyclerView.setLayoutManager(mLayoutManager);
        }
    }

}

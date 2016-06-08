package br.com.adley.myseriesproject.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.adley.myseriesproject.R;
import br.com.adley.myseriesproject.library.AppConsts;
import br.com.adley.myseriesproject.library.RecyclerItemClickListener;
import br.com.adley.myseriesproject.library.Utils;
import br.com.adley.myseriesproject.models.TVShow;
import br.com.adley.myseriesproject.models.TVShowDetails;
import br.com.adley.myseriesproject.models.TVShowSeasons;
import br.com.adley.myseriesproject.themoviedb.adapters.ListSeasonRecyclerViewAdapter;
import br.com.adley.myseriesproject.themoviedb.service.GetTVShowDetailsJsonData;
import br.com.adley.myseriesproject.themoviedb.service.GetTVShowSeasonJsonData;

/***
 * An activity to exhibit a details for a specif show
 */
public class DetailsTVShowActivity extends BaseActivity {

    private TVShowDetails mTVShowDetails;
    private TextView mTVShowTitle;
    private TextView mTVShowSynopsis;
    private ImageView mTVShowPoster;
    private TextView mTVShowRatingNumber;
    private TextView mTVShowNextDateNameEpisode;
    private TextView mTVShowDetailsNoSeason;
    private TVShow mTVShow;
    private List<TVShowSeasons> mTVShowSeasons;
    private ProgressDialog mProgress;
    private RecyclerView mRecyclerViewSeason;
    private ListSeasonRecyclerViewAdapter mListSeasonRecyclerViewAdapter;
    private View mTVShowDetailsView;
    private FloatingActionButton mFab;
    private String mRestoredFavorites;
    private SharedPreferences mSharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tvshow_details);
        activateToolbarWithHomeEnabled();

        // Get View Elements
        mTVShowTitle = (TextView) findViewById(R.id.title_tvshow_detail);
        mTVShowSynopsis = (TextView) findViewById(R.id.synopsis_tvshow);
        mTVShowPoster = (ImageView) findViewById(R.id.introduction_image_background);

        // Checks the orientation of the screen
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTVShowPoster.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mTVShowPoster.setScaleType(ImageView.ScaleType.FIT_XY);
        }

        mTVShowRatingNumber = (TextView) findViewById(R.id.note_number_tvshow);
        mTVShowNextDateNameEpisode = (TextView) findViewById(R.id.next_episode_date_name);
        mTVShowDetailsNoSeason = (TextView) findViewById(R.id.no_list_season_error);
        mFab = (FloatingActionButton) findViewById(R.id.add_show_float);
        mTVShowDetailsView = findViewById(R.id.activity_tvshow_details);
        mSharedPref = getSharedPreferences(AppConsts.FAVORITES_SHAREDPREFERENCES_KEY, Context.MODE_PRIVATE);
        mRestoredFavorites = mSharedPref.getString(AppConsts.FAVORITES_SHAREDPREFERENCES_KEY, null);

        mTVShowSeasons = new ArrayList<>();
        mRecyclerViewSeason = (RecyclerView) findViewById(R.id.recycler_view_season_list);
        mRecyclerViewSeason.setLayoutManager(new LinearLayoutManager(this));
        mListSeasonRecyclerViewAdapter = new ListSeasonRecyclerViewAdapter(DetailsTVShowActivity.this, new ArrayList<TVShowSeasons>());
        mRecyclerViewSeason.setAdapter(mListSeasonRecyclerViewAdapter);

        mRecyclerViewSeason.addOnItemTouchListener(new RecyclerItemClickListener(this,
                mRecyclerViewSeason, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //Toast.makeText(DetailsTVShowActivity.this, "Não implementado.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemLongClick(View view, int position) {
                //Toast.makeText(DetailsTVShowActivity.this, "Não implementado.", Toast.LENGTH_SHORT).show();
            }
        }));

        Intent intent = getIntent();
        mTVShow = (TVShow) intent.getSerializableExtra(AppConsts.TVSHOW_TRANSFER);

        //Load Shared Preferences
        loadConfigPreferences();

        //Get Show Details Data
        ProcessTVShowsDetails processTVShowsDetails = new ProcessTVShowsDetails(mTVShow, isLanguageUsePtBr());
        processTVShowsDetails.execute();

    }

    // Process and execute data into recycler view
    public class ProcessTVShowsDetails extends GetTVShowDetailsJsonData {
        private ProcessData processData;

        public ProcessTVShowsDetails(TVShow show, boolean isLanguagePtBr) {
            super(show, DetailsTVShowActivity.this, isLanguagePtBr);
        }

        public void execute() {
            // Start process data (download and get)
            processData = new ProcessData();
            processData.execute();

            // Start loading dialog
            mProgress = Utils.configureProgressDialog(getString(R.string.loading_show_title), getString(R.string.loading_seasons_description), true, true, DetailsTVShowActivity.this);
            mProgress.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel_button_progress_dialog), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    processData.cancel(true);
                    DetailsTVShowActivity.this.finish();
                }
            });
            mProgress.setCancelable(false);
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.show();
        }

        public class ProcessData extends DownloadJsonData {
            protected void onPostExecute(String webData) {
                super.onPostExecute(webData);
                mTVShowDetails = getTVShowsDetails();
                changeFabButton();
                //Get and Process SeasonData
                if (mTVShowDetails.getNumberOfSeasons() > 0) {
                    Utils.setLayoutInvisible(mTVShowDetailsNoSeason);
                    mProgress.setMessage(getString(R.string.loading_seasons_description));
                    for (int seasonNumber = 1; seasonNumber <= mTVShowDetails.getNumberOfSeasons(); seasonNumber++) {
                        ProcessSeason processSeason = new ProcessSeason(mTVShowDetails.getId(), seasonNumber);
                        processSeason.execute();
                    }
                } else {
                    mProgress.dismiss();
                    bindParams();
                    bindFABAdd();
                }
            }
        }
    }

    // Process Season Data
    public class ProcessSeason extends GetTVShowSeasonJsonData {

        public ProcessSeason(int showId, int serieNumber) {
            super(showId, serieNumber, DetailsTVShowActivity.this);
        }

        public void execute() {
            // Start process data (download and get)
            ProcessData processData = new ProcessData();
            processData.execute();
        }

        public class ProcessData extends DownloadJsonData {
            protected void onPostExecute(String webData) {
                super.onPostExecute(webData);
                mTVShowSeasons.add(getTVShowSeasons());
                if (getSeasonNumberTVShow() == mTVShowDetails.getNumberOfSeasons()) {
                    mListSeasonRecyclerViewAdapter.loadNewData(mTVShowSeasons);
                    mProgress.dismiss();
                    bindParams();
                    bindFABAdd();
                }
            }
        }
    }

    private void changeFabButton() {
        // Check if the show has already has added
        mRestoredFavorites = mSharedPref.getString(AppConsts.FAVORITES_SHAREDPREFERENCES_KEY, null);
        if (mRestoredFavorites != null) {
            List<Integer> ids = Utils.convertStringToIntegerList(AppConsts.FAVORITES_SHAREDPREFERENCES_DELIMITER, mRestoredFavorites);
            if (Utils.checkItemInIntegerList(ids, mTVShowDetails.getId())) {
                mFab.setBackgroundTintList(ColorStateList.valueOf(Color
                        .parseColor(getString(R.string.red_color_string))));
                mFab.setImageDrawable(ContextCompat.getDrawable(DetailsTVShowActivity.this, android.R.drawable.ic_delete));
            } else {
                mFab.setBackgroundTintList(ColorStateList.valueOf(Color
                        .parseColor(getString(R.string.green_color_string))));
                mFab.setImageDrawable(ContextCompat.getDrawable(DetailsTVShowActivity.this, android.R.drawable.ic_input_add));
            }
        }
    }

    private void changeFabButton(boolean isLastItem) {
        // Check if the show has already has added
        mFab.setBackgroundTintList(ColorStateList.valueOf(Color
                .parseColor(getString(R.string.green_color_string))));
        mFab.setImageDrawable(ContextCompat.getDrawable(DetailsTVShowActivity.this, android.R.drawable.ic_input_add));
    }

    /**
     * Bind Parameters after download data.
     */
    private void bindFABAdd() {
        Utils.setLayoutVisible(mFab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRestoredFavorites = mSharedPref.getString(AppConsts.FAVORITES_SHAREDPREFERENCES_KEY, null);
                SharedPreferences.Editor spEditor = mSharedPref.edit();
                if (mRestoredFavorites != null) {
                    List<Integer> idShowList = Utils.convertStringToIntegerList(AppConsts.FAVORITES_SHAREDPREFERENCES_DELIMITER, mRestoredFavorites);

                    if (Utils.checkItemInIntegerList(idShowList, mTVShowDetails.getId())) {
                        // Delete show
                        idShowList = Utils.removeIntegerItemFromList(idShowList, mTVShowDetails.getId());
                        String idsResult = Utils.convertListToString(AppConsts.FAVORITES_SHAREDPREFERENCES_DELIMITER, idShowList);
                        spEditor.putString(AppConsts.FAVORITES_SHAREDPREFERENCES_KEY, idsResult);
                        spEditor.apply();
                        if (idsResult == null) {
                            changeFabButton(true);
                        } else {
                            changeFabButton();
                        }
                        Utils.createSnackbar(Color.RED, getString(R.string.success_remove_show),mTVShowDetailsView);
                    } else {
                        // Add show when the list is not null
                        idShowList.add(mTVShowDetails.getId());
                        String idsResult = Utils.convertListToString(AppConsts.FAVORITES_SHAREDPREFERENCES_DELIMITER, idShowList);
                        spEditor.putString(AppConsts.FAVORITES_SHAREDPREFERENCES_KEY, idsResult);
                        spEditor.apply();
                        changeFabButton();
                        Utils.createSnackbar(Color.GREEN, getString(R.string.success_add_new_show), mTVShowDetailsView);

                    }

                } else {
                    // Add show when the list is null
                    spEditor.putString(AppConsts.FAVORITES_SHAREDPREFERENCES_KEY, String.valueOf(mTVShowDetails.getId()));
                    spEditor.apply();
                    changeFabButton();
                    Utils.createSnackbar(Color.GREEN, getString(R.string.success_add_new_show),mTVShowDetailsView);
                }

            }
        });
    }

    private void bindParams() {
        if (mProgress.isShowing()) mProgress.dismiss();
        if (mTVShowDetails.getOriginalName() != null && mTVShowDetails.getOverview() != null) {
            if (mTVShowTitle != null) {
                mTVShowTitle.setText(mTVShowDetails.getOriginalName());
            }

            if (mTVShowSynopsis != null) {
                mTVShowSynopsis.setText(Html.fromHtml(mTVShowDetails.getOverview()));
            }

            if (mTVShowRatingNumber != null) {
                if (mTVShowDetails.getVoteAverage() > 0.0 & mTVShowDetails.getVoteCount() > 0) {
                    Locale ptBr = new Locale("pt", "BR");
                    mTVShowRatingNumber.setText(String.format(ptBr, "%.2f", mTVShowDetails.getVoteAverage()));
                } else {
                    mTVShowRatingNumber.setText(getString(R.string.abbreviation_do_not_have));
                }
            } else {
                mTVShowRatingNumber.setText(getString(R.string.error_generic_message));
            }

            // Get next episode name and date
            if (mTVShowNextDateNameEpisode != null) {
                // Pass the last season, show and activity
                if (mTVShowDetails.getNumberOfSeasons() == 0) {
                    Utils.setLayoutVisible(mTVShowDetailsNoSeason);
                    if (mTVShowDetails.getInProduction()) {
                        mTVShowNextDateNameEpisode.setText(getString(R.string.warning_no_next_episode));
                    } else {
                        mTVShowNextDateNameEpisode.setText(getString(R.string.no_more_in_production));
                    }
                } else {
                    Utils.setNextEpisode(mTVShowSeasons.get(mTVShowSeasons.size() - 1), mTVShowDetails, DetailsTVShowActivity.this);
                    mTVShowNextDateNameEpisode.setText(mTVShowDetails.getNextEpisode());
                }
                //mTVShowNextDateEpisode.setMovementMethod(LinkMovementMethod.getInstance());
            }

            if (mTVShowDetails.getBackdropPath() != null) {
                Picasso.with(DetailsTVShowActivity.this)
                        .load(mTVShowDetails.getBackdropPath())
                        .into(mTVShowPoster);
            } else if (mTVShowDetails.getPosterPath() != null && mTVShowDetails.getBackdropPath() == null) {
                Picasso.with(DetailsTVShowActivity.this)
                        .load(mTVShowDetails.getPosterPath())
                        .into(mTVShowPoster);
            }
        } else {
            Toast.makeText(this, getString(R.string.error_generic_message), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTVShowPoster.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mTVShowPoster.setScaleType(ImageView.ScaleType.FIT_XY);
        }
    }

}

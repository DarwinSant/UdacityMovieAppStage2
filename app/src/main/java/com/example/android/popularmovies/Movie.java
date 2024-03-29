package com.example.android.popularmovies;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;


import com.example.android.popularmovies.database.MovieContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

class Movie implements Parcelable

{
    static final String EXTRA_MOVIE = "com.example.EXTRA_MOVIE";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE ="title";
    private static final String KEY_OVERVIEW = "overview";
    private static final String KEY_POSTER_PATH ="poster_path";
    private static final String KEY_VOTE_COUNT = "vote_count";
    private static final String KEY_VOTE_AVERAGE = "vote_average";
    private static final String KEY_RELEASE_DATE = "release_date";

    public final long id;
    public final String title;
    final String overview;
    private final String poster_path;
    final double vote_average;
    private final long vote_count;
    final String release_date;
    private ArrayList<Trailer> trailers;
    private ArrayList<Review> reviews;
    private Bitmap poster;

    Movie(long id, String title, String overview, String poster_path, double vote_average, long vote_count, String release_date)
    {
        this.id=id;
        this.title=title;
        this.overview=overview;
        this.poster_path=poster_path;
        this.vote_average=vote_average;
        this.vote_count=vote_count;
        this.release_date=release_date;
        this.trailers = new ArrayList<>();
        this.reviews = new ArrayList<>();
    }


    Bitmap getPoster() {
        return poster;
    }

    void setPoster(Bitmap poster) {
        this.poster = poster;
    }
    void setPosterFromCursor(Cursor cursor){
        byte[] bytes = cursor.getBlob(cursor.getColumnIndex(MovieContract.MovieEntry.MOVIE_POSTER));
        ByteArrayInputStream posterStream = new ByteArrayInputStream(bytes);
        this.poster = BitmapFactory.decodeStream(posterStream);
    }

    void setTrailers(ArrayList<Trailer> trailers) {
        this.trailers = trailers;
    }

    void setReviews(ArrayList<Review> reviews) {
        this.reviews = reviews;
    }


    Movie(Bundle bundle)
    {
        this(bundle.getLong(KEY_ID),
                                bundle.getString(KEY_TITLE),
                                bundle.getString(KEY_OVERVIEW),
                                bundle.getString(KEY_POSTER_PATH),
                                bundle.getDouble(KEY_VOTE_AVERAGE),
                                bundle.getLong(KEY_VOTE_COUNT),
                                bundle.getString(KEY_RELEASE_DATE));

    }


    private Movie(Parcel in) {
        id = in.readLong();
        title = in.readString();
        overview = in.readString();
        poster_path = in.readString();
        vote_average = in.readDouble();
        vote_count = in.readLong();
        release_date = in.readString();
    }

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    Bundle toBundle()
    {
        Bundle bundle = new Bundle();
        bundle.putLong(KEY_ID, id);
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_OVERVIEW, overview);
        bundle.putString(KEY_POSTER_PATH, poster_path);
        bundle.putDouble(KEY_VOTE_AVERAGE, vote_average);
        bundle.putLong(KEY_VOTE_COUNT, vote_count);
        bundle.putString(KEY_RELEASE_DATE,release_date);
        return bundle;
    }

    static Movie getMovieFromJson(JSONObject jsonObject) throws JSONException
    {
        return new Movie(jsonObject.getLong(KEY_ID),
                jsonObject.getString(KEY_TITLE),
                jsonObject.getString(KEY_OVERVIEW),
                jsonObject.getString(KEY_POSTER_PATH),
                jsonObject.getDouble(KEY_VOTE_AVERAGE),
                jsonObject.getLong(KEY_VOTE_COUNT),
                jsonObject.getString(KEY_RELEASE_DATE));
    }

    //returns the uri needed for Picasso.

    Uri getPosterUri(String size)
    {
        final String BASE_URL = "http://image.tmdb.org/t/p";

        return Uri.parse(BASE_URL).buildUpon().appendPath(size).appendEncodedPath(poster_path).build();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(overview);
        dest.writeString(poster_path);
        dest.writeDouble(vote_average);
        dest.writeLong(vote_count);
        dest.writeString(release_date);
    }


    boolean saveToBookmarks(Context context){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MovieContract.MovieEntry.MOVIE_ID, this.id);
        contentValues.put(MovieContract.MovieEntry.MOVIE_TITLE, this.title);
        contentValues.put(MovieContract.MovieEntry.MOVIE_OVERVIEW, this.overview);
        contentValues.put(MovieContract.MovieEntry.MOVIE_POSTER_PATH, this.poster_path);
        contentValues.put(MovieContract.MovieEntry.MOVIE_VOTES, this.vote_count);
        contentValues.put(MovieContract.MovieEntry.MOVIE_AVG, this.vote_average);
        contentValues.put(MovieContract.MovieEntry.MOVIE_RELEASE_DATE, this.release_date);
        contentValues.put(MovieContract.MovieEntry.MOVIE_TRAILERS,Trailer.arrayToString(trailers));
        contentValues.put(MovieContract.MovieEntry.MOVIE_REVIEWS,Review.arrayToString(reviews));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        this.poster.compress(Bitmap.CompressFormat.JPEG,100,bos);
        byte[] bytes = bos.toByteArray();

        contentValues.put(MovieContract.MovieEntry.MOVIE_POSTER,bytes);

        if (context.getContentResolver().insert(MovieContract.MovieEntry.CONTENT_URI,contentValues)!=null){
            Toast.makeText(context, R.string.bookmark_added,Toast.LENGTH_SHORT).show();
            return true;
        }else{
            Toast.makeText(context, R.string.bookmark_insert_error,Toast.LENGTH_SHORT).show();
            return false;
        }

    }

    boolean removeFromBookmarks(Context context){
        long deletedRows = context.getContentResolver().delete(MovieContract.MovieEntry.CONTENT_URI,
                MovieContract.MovieEntry.MOVIE_ID + "=?",new String[]{Long.toString(this.id)});
        if (deletedRows>0){
            Toast.makeText(context, R.string.bookmark_deleted,Toast.LENGTH_SHORT).show();
            return true;
        }else {
            Toast.makeText(context, R.string.bookmark_delete_error, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    boolean isBookmarked(Context context){
        Cursor cursor = context.getContentResolver()
                .query(MovieContract.MovieEntry.CONTENT_URI,
                        new String[]{MovieContract.MovieEntry.MOVIE_ID},
                        MovieContract.MovieEntry.MOVIE_ID + "=?",
                        new String[]{Long.toString(this.id)},null);
        if (cursor!=null) {
            boolean bookmarked = cursor.getCount() > 0;
            cursor.close();
            return bookmarked;
        }
        return false;
    }


}

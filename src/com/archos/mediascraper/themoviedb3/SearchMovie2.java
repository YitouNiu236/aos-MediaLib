// Copyright 2020 Courville Software
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.mediascraper.themoviedb3;

import android.util.Log;

import com.archos.mediascraper.FileFetcher;
import com.archos.mediascraper.FileFetcher.FileFetchResult;
import com.archos.mediascraper.ScrapeStatus;
import com.archos.mediascraper.SearchResult;
import com.archos.mediascraper.xml.MovieScraper3;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.MovieResultsPage;
import com.uwetrottmann.tmdb2.services.MoviesService;
import com.uwetrottmann.tmdb2.services.SearchService;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

/*
 * /3/search/movie
 *
 * Required Parameters
 * api_key
 * query            CGI escaped string
 *
 * Optional Parameters
 * page
 * language         ISO 639-1 code.
 * include_adult    Toggle the inclusion of adult titles.
 * year             Filter results to only include this value.
 */
public class SearchMovie2 {
    private static final String TAG = SearchMovie2.class.getSimpleName();
    private static final boolean DBG = false;

    public static SearchMovieResult search(String query, String language, String year, int resultLimit, SearchService searchService) {
        SearchMovieResult myResult = new SearchMovieResult();
        Response<Movie> movieResponse = null;

        List<SearchResult> parserResult = null;
        Response<MovieResultsPage> response = null;

        Integer annee = null;
        try {
            annee = Integer.parseInt(year);
        } catch(NumberFormatException nfe) {
            Log.w(TAG, "getMatches2: searchInfo.getYear() is not an integer");
            annee = null;
        }
        if (DBG) Log.d(TAG, "searchMovie: quering tmdb for " + query + " year " + year + " in " + language);
        try {
            response = searchService.movie(query, null, language,
                    null, true, annee, null).execute();
            switch (response.code()) {
                case 401: // auth issue
                    if (DBG) Log.d(TAG, "search: auth error");
                    myResult.status = ScrapeStatus.AUTH_ERROR;
                    //TODO: MovieScraper3.reauth();
                    return myResult;
                case 404: // not found
                    // TODO: check year parsing because scraping still put a ( and do not remove it
                    myResult.status = ScrapeStatus.NOT_FOUND;
                    if (year != null) {
                        if (DBG) Log.d(TAG, "search: retrying search for '" + query + "' without year.");
                        return search(query, language, null, resultLimit, searchService);
                    }
                    if (DBG) Log.d(TAG, "search: " + query + " not found");
                    break;
                default:
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            parserResult = SearchMovieParser2.getResult(response, resultLimit);
                            myResult.result = parserResult;
                            myResult.status = ScrapeStatus.OKAY;
                        } else {
                            myResult.status = ScrapeStatus.NOT_FOUND;
                        }
                    } else { // an error at this point is PARSER related
                        if (DBG) Log.d(TAG, "search: response is not successful for " + query);
                        myResult.status = ScrapeStatus.ERROR_PARSER;
                    }
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "searchMovie: caught IOException");
            if (DBG) Log.e(TAG, e.getMessage(), e);
            myResult.result = SearchMovieResult.EMPTY_LIST;
            myResult.status = ScrapeStatus.ERROR_PARSER;
            myResult.reason = e;
        }
        return myResult;
    }
}

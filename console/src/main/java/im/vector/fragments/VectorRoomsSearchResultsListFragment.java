/*
 * Copyright 2015 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import org.matrix.androidsdk.adapters.MessageRow;
import org.matrix.androidsdk.adapters.MessagesAdapter;
import org.matrix.androidsdk.fragments.IconAndTextDialogFragment;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.util.EventDisplay;

import java.util.ArrayList;
import java.util.List;

import im.vector.R;
import im.vector.adapters.VectorRoomsSearchResultsAdapter;

public class VectorRoomsSearchResultsListFragment  extends ConsoleMessageListFragment {

    public static VectorRoomsSearchResultsListFragment newInstance(String matrixId, int layoutResId) {
        VectorRoomsSearchResultsListFragment f = new VectorRoomsSearchResultsListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_ID, layoutResId);
        args.putString(ARG_MATRIX_ID, matrixId);
        f.setArguments(args);
        return f;
    }

    @Override
    public MessagesAdapter createMessagesAdapter() {
        return new VectorRoomsSearchResultsAdapter(mSession, getActivity(), getMXMediasCache());
    }

    /**
     * The user scrolls the list.
     * Apply an expected behaviour
     * @param event the scroll event
     */
    @Override
    public void onListTouch(MotionEvent event) {
    }

    /**
     * return true to display all the events.
     * else the unknown events will be hidden.
     */
    @Override
    public boolean isDisplayAllEvents() {
        return true;
    }

    /**
     * Display a global spinner or any UI item to warn the user that there are some pending actions.
     */
    @Override
    public void displayLoadingProgress() {
        if (null != getActivity()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (null != getActivity()) {
                        final View progressView = getActivity().findViewById(R.id.search_load_oldest_progress);

                        if (null != progressView) {
                            progressView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }

    /**
     * Dismiss any global spinner.
     */
    @Override
    public void dismissLoadingProgress() {
        if (null != getActivity()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (null != getActivity()) {
                        final View progressView = getActivity().findViewById(R.id.search_load_oldest_progress);

                        if (null != progressView) {
                            progressView.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }

    /**
     * Scroll the fragment to the bottom
     */
    public void scrollToBottom() {
        if (0 != mAdapter.getCount()) {
            mMessageListView.setSelection(mAdapter.getCount() - 1);
        }
    }

    /**
     * Update the searched pattern.
     * @param pattern the pattern to find out. null to disable the search mode
     */
    public void searchPattern(final String pattern,  final OnSearchResultListener onSearchResultListener) {
        // ConsoleMessageListFragment displays the list of unfiltered messages when there is no pattern
        // in the search case, clear the list
        if (TextUtils.isEmpty(pattern)) {
            mPattern = null;
            mAdapter.clear();

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onSearchResultListener.onSearchSucceed(0);
                }
            });
        } else {

            super.searchPattern(pattern, new OnSearchResultListener() {
                @Override
                public void onSearchSucceed(int nbrMessages) {
                    // scroll to the bottom
                    scrollToBottom();

                    if (null != onSearchResultListener) {
                        onSearchResultListener.onSearchSucceed(nbrMessages);
                    }
                }

                @Override
                public void onSearchFailed() {
                    // clear the results list if teh search fails
                    mAdapter.clear();

                    if (null != onSearchResultListener) {
                        onSearchResultListener.onSearchFailed();
                    }
                }
            });
        }
    }

    public Boolean onRowLongClick(int position) {
        final MessageRow messageRow = mAdapter.getItem(position);
        final List<Integer> textIds = new ArrayList<>();
        final List<Integer> iconIds = new ArrayList<Integer>();

        textIds.add(R.string.copy);
        iconIds.add(R.drawable.ic_material_copy);

        // display the JSON
        textIds.add(R.string.message_details);
        iconIds.add(R.drawable.ic_material_description);

        FragmentManager fm = getActivity().getSupportFragmentManager();
        IconAndTextDialogFragment fragment = (IconAndTextDialogFragment) fm.findFragmentByTag(TAG_FRAGMENT_MESSAGE_OPTIONS);

        if (fragment != null) {
            fragment.dismissAllowingStateLoss();
        }

        Integer[] lIcons = iconIds.toArray(new Integer[iconIds.size()]);
        Integer[] lTexts = textIds.toArray(new Integer[iconIds.size()]);

        fragment = IconAndTextDialogFragment.newInstance(lIcons, lTexts);
        fragment.setOnClickListener(new IconAndTextDialogFragment.OnItemClickListener() {
            @Override
            public void onItemClick(IconAndTextDialogFragment dialogFragment, int position) {
                final Integer selectedVal = textIds.get(position);

                if (selectedVal == R.string.copy) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                            Event event = messageRow.getEvent();
                            EventDisplay display = new EventDisplay(getActivity(), event, null);

                            ClipData clip = ClipData.newPlainText("", display.getTextualDisplay().toString());
                            clipboard.setPrimaryClip(clip);
                        }
                    });
                } else if (selectedVal == R.string.message_details) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FragmentManager fm =  getActivity().getSupportFragmentManager();

                            MessageDetailsFragment fragment = (MessageDetailsFragment) fm.findFragmentByTag(TAG_FRAGMENT_MESSAGE_DETAILS);
                            if (fragment != null) {
                                fragment.dismissAllowingStateLoss();
                            }
                            fragment = MessageDetailsFragment.newInstance(messageRow.getEvent().toString());
                            fragment.show(fm, TAG_FRAGMENT_MESSAGE_DETAILS);
                        }
                    });
                }
            }
        });

        fragment.show(fm, TAG_FRAGMENT_MESSAGE_OPTIONS);

        return true;
    }

    /**
     * Called when a long click is performed on the message content
     * @param position the cell position
     * @return true if managed
     */
    public Boolean onContentLongClick(int position) {
        return onRowLongClick(position);
    }

}

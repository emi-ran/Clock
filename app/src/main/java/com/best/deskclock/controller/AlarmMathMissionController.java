// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.controller;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmActivity;
import com.best.deskclock.alarms.AlarmMathChallenge;
import com.best.deskclock.databinding.AlarmMathChallengeDialogBinding;
import com.best.deskclock.uicomponents.CustomDialog;

import java.util.Random;

public class AlarmMathMissionController {
    private final Context mContext;
    private final Callback mCallback;
    private final Typeface mGeneralBoldTypeface;
    private AlertDialog mCurrentDialog;

    public AlarmMathMissionController(@NonNull Context context, @NonNull Callback callback, @NonNull Typeface generalBoldTypeface) {
        mContext = context;
        mCallback = callback;
        mGeneralBoldTypeface = generalBoldTypeface;
    }

    public void requestMissionAction(int action, @NonNull String mathHardnessLevel, int questionCount, @NonNull Random random) {
        if (mCurrentDialog != null && mCurrentDialog.isShowing()) {
            return;
        }

        final int[] remaining = { Math.max(1, questionCount) };
        final AlarmMathChallenge[] activeChallenge = { AlarmMathChallenge.create(mathHardnessLevel, random) };

        final AlarmMathChallengeDialogBinding binding = AlarmMathChallengeDialogBinding.inflate(LayoutInflater.from(mContext));

        binding.mathChallengeTitle.setTypeface(mGeneralBoldTypeface);
        binding.mathChallengeTitle.setText(mContext.getString(action == AlarmActivity.MISSION_ACTION_SNOOZE
            ? R.string.math_challenge_title_snooze
            : R.string.math_challenge_title_dismiss));

        binding.mathChallengeText.setTypeface(mGeneralBoldTypeface);
        binding.mathChallengeText.setText(
            mContext.getString(R.string.math_challenge_prompt, activeChallenge[0].left(), activeChallenge[0].right())
        );

        binding.answerInputLayout.setTypeface(mGeneralBoldTypeface);
        binding.answerInput.setTypeface(mGeneralBoldTypeface);
        binding.answerInput.requestFocus();
        binding.answerInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(@Nullable Editable s) {}

            @Override
            public void beforeTextChanged(@Nullable CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(@Nullable CharSequence s, int start, int before, int count) {
                if (binding.mathChallengeError.getVisibility() != INVISIBLE) {
                    binding.mathChallengeError.setVisibility(INVISIBLE);
                }
            }
        });

        binding.mathChallengeError.setVisibility(INVISIBLE);
        binding.mathChallengeError.setTypeface(mGeneralBoldTypeface, Typeface.BOLD);

        mCurrentDialog = CustomDialog.create(
            mContext,
            null,
            null,
            null,
            null,
            binding.getRoot(),
            mContext.getString(android.R.string.ok),
            null,
            mContext.getString(android.R.string.cancel),
            null,
            mContext.getString(R.string.math_challenge_new_problem),
            null,
            alertDialog -> {
                alertDialog.setCancelable(false);

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
                    validateAnswer(binding, activeChallenge, remaining, mathHardnessLevel, random, alertDialog, action));

                binding.answerInput.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        validateAnswer(binding, activeChallenge, remaining, mathHardnessLevel, random, alertDialog, action);

                        return true;
                    }

                    return false;
                });

                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    activeChallenge[0] = AlarmMathChallenge.create(mathHardnessLevel, random);

                    binding.mathChallengeText.setText(mContext.getString(R.string.math_challenge_prompt,
                        activeChallenge[0].left(), activeChallenge[0].right()));

                    binding.answerInput.setText("");
                    binding.mathChallengeError.setVisibility(INVISIBLE);

                    binding.answerInput.requestFocus();
                });
            },
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );

        mCurrentDialog.show();
    }

    private void validateAnswer(@NonNull AlarmMathChallengeDialogBinding binding,
                                @NonNull AlarmMathChallenge[] challenge, @NonNull int[] remaining,
                                @NonNull String hardness, @NonNull Random random,
                                @NonNull AlertDialog dialog, int action) {
        final String answer = binding.answerInput.getText() == null ? "" : binding.answerInput.getText().toString().trim();
        if (!challenge[0].matches(answer)) {
            binding.mathChallengeError.setVisibility(VISIBLE);
            binding.answerInput.selectAll();
        } else if (--remaining[0] > 0) {
            challenge[0] = AlarmMathChallenge.create(hardness, random);
            binding.mathChallengeText.setText(mContext.getString(R.string.math_challenge_prompt,
                challenge[0].left(), challenge[0].right()));
            binding.answerInput.setText("");
            binding.mathChallengeError.setVisibility(INVISIBLE);
            binding.answerInput.requestFocus();
        } else {
            dialog.dismiss();
            mCurrentDialog = null;
            mCallback.onMissionResolved(action);
        }
    }

    public interface Callback {
        void onMissionResolved(int action);
    }

}

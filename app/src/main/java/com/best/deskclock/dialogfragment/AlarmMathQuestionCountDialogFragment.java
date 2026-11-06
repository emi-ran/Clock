// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.alarms.MathQuestionCount;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

/** Sets the number of questions independently for an individual alarm. */
public class AlarmMathQuestionCountDialogFragment extends DialogFragment {
    private static final String TAG = "math_question_count_dialog";
    private static final String ARG_COUNT = "count";
    private static final String STATE_INPUT = "input";
    public static final String REQUEST_KEY = "math_question_count_request";
    public static final String RESULT_COUNT = "math_question_count_result";
    private EditText mInput;

    @NonNull
    public static AlarmMathQuestionCountDialogFragment newInstance(int count) {
        Bundle args = new Bundle();
        args.putInt(ARG_COUNT, count);
        AlarmMathQuestionCountDialogFragment fragment = new AlarmMathQuestionCountDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static void show(@NonNull FragmentManager manager, @NonNull AlarmMathQuestionCountDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mInput != null) outState.putString(STATE_INPUT, mInput.getText().toString());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LinearLayout container = new LinearLayout(requireContext());
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 0, padding, 0);
        mInput = new EditText(requireContext());
        mInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        mInput.setSingleLine(true);
        mInput.setTypeface(ThemeUtils.loadFont(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(requireContext()))));
        mInput.setContentDescription(getString(R.string.math_question_count_title));
        mInput.setText(savedInstanceState == null ? String.valueOf(requireArguments().getInt(ARG_COUNT, 1))
            : savedInstanceState.getString(STATE_INPUT, "1"));
        mInput.selectAll();
        container.addView(mInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return CustomDialog.create(requireContext(), null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_calculate),
            getString(R.string.math_question_count_title), null, container,
            getString(android.R.string.ok), null, getString(android.R.string.cancel), null, null, null,
            dialog -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    int count = Integer.parseInt(mInput.getText().toString());
                    if (count < 1 || count > MathQuestionCount.MAX) throw new NumberFormatException();
                    Bundle result = new Bundle();
                    result.putInt(RESULT_COUNT, count);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                    dismiss();
                } catch (NumberFormatException e) {
                    mInput.setError(getString(R.string.math_question_count_invalid));
                }
            }), CustomDialog.SoftInputMode.SHOW_KEYBOARD);
    }

    @Override
    public void onDestroyView() {
        mInput = null;
        super.onDestroyView();
    }
}

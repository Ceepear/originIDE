package com.myopicmobile.textwarrior.android;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.InputType;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.CharacterPickerDialog;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Scroller;

import com.myopicmobile.textwarrior.base.BaseLanguage;
import com.myopicmobile.textwarrior.base.BaseLexer;
import com.myopicmobile.textwarrior.base.BasePanel;
import com.myopicmobile.textwarrior.bean.BlockLine;
import com.myopicmobile.textwarrior.bean.ColorLine;
import com.myopicmobile.textwarrior.bean.DeviderLine;
import com.myopicmobile.textwarrior.common.Document;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Pair;
import com.myopicmobile.textwarrior.common.TextWarriorException;
import com.myopicmobile.textwarrior.interfaces.OnSelectionChangedListener;
import com.myopicmobile.textwarrior.interfaces.RowListener;
import com.myopicmobile.textwarrior.interfaces.TextChangeListener;
import com.myopicmobile.textwarrior.language.java.JavaAutoCompletePanel;
import com.myopicmobile.textwarrior.language.java.JavaLexer;
import com.myopicmobile.textwarrior.language.java.JavaType;
import com.myopicmobile.textwarrior.language.java.LanguageJava;
import com.myopicmobile.textwarrior.scheme.ColorScheme;
import com.myopicmobile.textwarrior.scheme.ColorScheme.Colorable;
import com.myopicmobile.textwarrior.scheme.ColorSchemeLight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FreeScrollingTextField extends View
        implements Document.TextFieldMetrics {

    //---------------------------------------------------------------------
    //--------------------------  Caret Scroll  ---------------------------
    public final static int SCROLL_UP = 0;
    public final static int SCROLL_DOWN = 1;
    public final static int SCROLL_LEFT = 2;
    public final static int SCROLL_RIGHT = 3;
    private static final boolean DEBUG = false;
    protected static int DEFAULT_TAB_LENGTH_SPACES = 4;
    protected static int BASE_TEXT_SIZE_PIXELS = 16;
    protected static float EMPTY_CARET_WIDTH_SCALE = 0.75f;
    protected static float SEL_CARET_HEIGHT_SCALE = 0.5f;
    protected static long SCROLL_PERIOD = 250; //in milliseconds
    /*
     * Hash map for determining which characters to let the user choose from when
     * a hardware key is long-pressed. For example, long-pressing "e" displays
     * choices of "é, è, ê, ë" and so on.
     * This is biased towards European locales, but is standard Android behavior
     * for TextView.
     *
     * Copied from android.text.method.QwertyKeyListener, dated 2006
     */
    private static SparseArray<String> PICKER_SETS =
            new SparseArray<String>();

    static {
        PICKER_SETS.put('A', "\u00C0\u00C1\u00C2\u00C4\u00C6\u00C3\u00C5\u0104\u0100");
        PICKER_SETS.put('C', "\u00C7\u0106\u010C");
        PICKER_SETS.put('D', "\u010E");
        PICKER_SETS.put('E', "\u00C8\u00C9\u00CA\u00CB\u0118\u011A\u0112");
        PICKER_SETS.put('G', "\u011E");
        PICKER_SETS.put('L', "\u0141");
        PICKER_SETS.put('I', "\u00CC\u00CD\u00CE\u00CF\u012A\u0130");
        PICKER_SETS.put('N', "\u00D1\u0143\u0147");
        PICKER_SETS.put('O', "\u00D8\u0152\u00D5\u00D2\u00D3\u00D4\u00D6\u014C");
        PICKER_SETS.put('R', "\u0158");
        PICKER_SETS.put('S', "\u015A\u0160\u015E");
        PICKER_SETS.put('T', "\u0164");
        PICKER_SETS.put('U', "\u00D9\u00DA\u00DB\u00DC\u016E\u016A");
        PICKER_SETS.put('Y', "\u00DD\u0178");
        PICKER_SETS.put('Z', "\u0179\u017B\u017D");
        PICKER_SETS.put('a', "\u00E0\u00E1\u00E2\u00E4\u00E6\u00E3\u00E5\u0105\u0101");
        PICKER_SETS.put('c', "\u00E7\u0107\u010D");
        PICKER_SETS.put('d', "\u010F");
        PICKER_SETS.put('e', "\u00E8\u00E9\u00EA\u00EB\u0119\u011B\u0113");
        PICKER_SETS.put('g', "\u011F");
        PICKER_SETS.put('i', "\u00EC\u00ED\u00EE\u00EF\u012B\u0131");
        PICKER_SETS.put('l', "\u0142");
        PICKER_SETS.put('n', "\u00F1\u0144\u0148");
        PICKER_SETS.put('o', "\u00F8\u0153\u00F5\u00F2\u00F3\u00F4\u00F6\u014D");
        PICKER_SETS.put('r', "\u0159");
        PICKER_SETS.put('s', "\u00A7\u00DF\u015B\u0161\u015F");
        PICKER_SETS.put('t', "\u0165");
        PICKER_SETS.put('u', "\u00F9\u00FA\u00FB\u00FC\u016F\u016B");
        PICKER_SETS.put('y', "\u00FD\u00FF");
        PICKER_SETS.put('z', "\u017A\u017C\u017E");
        PICKER_SETS.put(KeyCharacterMap.PICKER_DIALOG_INPUT,
                "\u2026\u00A5\u2022\u00AE\u00A9\u00B1[]{}\\|");
        PICKER_SETS.put('/', "\\");

        // From packages/inputmethods/LatinIME/res/xml/kbd_symbols.xml

        PICKER_SETS.put('1', "\u00b9\u00bd\u2153\u00bc\u215b");
        PICKER_SETS.put('2', "\u00b2\u2154");
        PICKER_SETS.put('3', "\u00b3\u00be\u215c");
        PICKER_SETS.put('4', "\u2074");
        PICKER_SETS.put('5', "\u215d");
        PICKER_SETS.put('7', "\u215e");
        PICKER_SETS.put('0', "\u207f\u2205");
        PICKER_SETS.put('$', "\u00a2\u00a3\u20ac\u00a5\u20a3\u20a4\u20b1");
        PICKER_SETS.put('%', "\u2030");
        PICKER_SETS.put('*', "\u2020\u2021");
        PICKER_SETS.put('-', "\u2013\u2014");
        PICKER_SETS.put('+', "\u00b1");
        PICKER_SETS.put('(', "[{<");
        PICKER_SETS.put(')', "]}>");
        PICKER_SETS.put('!', "\u00a1");
        PICKER_SETS.put('"', "\u201c\u201d\u00ab\u00bb\u02dd");
        PICKER_SETS.put('?', "\u00bf");
        PICKER_SETS.put(',', "\u201a\u201e");
        PICKER_SETS.put('=', "\u2260\u2248\u221e");
        PICKER_SETS.put('<', "\u2264\u00ab\u2039");
        PICKER_SETS.put('>', "\u2265\u00bb\u203a");
    }

    private final Scroller mScroller;
    protected TouchNavigationMethod mNaviMethod;
    protected DocumentProvider mDoc; // the model in MVC
    protected int mCaretPosition = 0;
    protected int mSelectionAnchor = -1; // inclusive
    protected int mSelectionEdge = -1; // exclusive
    protected int mTabLength = DEFAULT_TAB_LENGTH_SPACES;
    protected int mAutoIndentWidth = 3;
    protected boolean isHighlightRow = false;
    protected boolean mShowNonPrinting = false;
    protected boolean isAutoIndent = true;
    protected boolean isLongPressCaps = false;
    protected boolean isEdited = false;
    protected BasePanel mAutoCompletePanel;
    protected ColorScheme mColorScheme = new ColorSchemeLight();
    private Context mContext;
    private OnSizeChangedListener onSizeChangedListener;
    private TextFieldController mFieldController; // the controller in MVC
    private TextFieldInputConnection mInputConnection;
    private RowListener mRowListener;
    private OnSelectionChangedListener mSelModeLis;
    private int mCaretRow = 0; // can be calculated, but stored for efficiency purposes
    private int _xExtent = 0;
    private int mLeftOffset = 0;
    private boolean isBlockLine, isWeekLine;
    //编辑器设置
    private boolean mShowLineNumbers = false;
    private boolean canAutoCompete = true;
    private boolean isLayout;
    private float mZoomFactor = 1;
    private int mCaretX;
    private int mCaretY;
    private int mTopOffset;
    private int mAlphaWidth;
    private int mSpaceWidth;
    private char mEmoji;
    private Paint mBrush, mBrushLine, mCodeLine, mColorLine;
    private final Runnable _scrollCaretDownTask = new Runnable() {
        @Override
        public void run() {
            mFieldController.moveCaretDown();
            if (!caretOnLastRowOfFile()) {
                postDelayed(_scrollCaretDownTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable _scrollCaretUpTask = new Runnable() {
        @Override
        public void run() {
            mFieldController.moveCaretUp();
            if (!caretOnFirstRowOfFile()) {
                postDelayed(_scrollCaretUpTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable _scrollCaretLeftTask = new Runnable() {
        @Override
        public void run() {
            mFieldController.moveCaretLeft(false);
            if (mCaretPosition > 0 &&
                    mCaretRow == mDoc.findRowNumber(mCaretPosition - 1)) {
                postDelayed(_scrollCaretLeftTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable _scrollCaretRightTask = new Runnable() {
        @Override
        public void run() {
            mFieldController.moveCaretRight(false);
            if (!caretOnEOF() &&
                    mCaretRow == mDoc.findRowNumber(mCaretPosition + 1)) {
                postDelayed(_scrollCaretRightTask, SCROLL_PERIOD);
            }
        }
    };
    private BaseLanguage mLanguage;
    private ClipboardPanel mClipboardPanel;
    private ClipboardManager mClipboardManager;
    private TextChangeListener mTextListener;
    private Pair mCaretSpan = new Pair(0, 0);
    private int identifierColor;
    private long mLastScroll;

    public FreeScrollingTextField(Context context) {
        super(context);
        this.mContext = context;
        mDoc = new DocumentProvider(this);
        mNaviMethod = new TouchNavigationMethod(this);
        mScroller = new Scroller(context);
        initView();
    }

    public FreeScrollingTextField(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        mDoc = new DocumentProvider(this);
        mNaviMethod = new TouchNavigationMethod(this);
        mScroller = new Scroller(context);
        initView();
    }

    public FreeScrollingTextField(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        mDoc = new DocumentProvider(this);
        mNaviMethod = new TouchNavigationMethod(this);
        mScroller = new Scroller(context);
        initView();
    }

    public BaseLanguage getLanguage() {
        return this.mLanguage;
    }

    public void setLanguage(BaseLanguage language) {
        this.mLanguage = language;
        mAutoCompletePanel = mLanguage.getCodePanel();
        setLexer(mLanguage.getLexer());
    }

    public int getTopOffset() {
        return mTopOffset;
    }

    public int getAutoIndentWidth() {
        return mAutoIndentWidth;
    }

    public void setAutoIndentWidth(int autoIndentWidth) {
        mAutoIndentWidth = autoIndentWidth;
    }

    public int getCaretY() {
        return mCaretY;
    }

    public int getCaretX() {
        return mCaretX;
    }

    public boolean isShowLineNumbers() {
        return mShowLineNumbers;
    }

    public void setShowLineNumbers(boolean showLineNumbers) {
        mShowLineNumbers = showLineNumbers;
    }

    public boolean isAutoCompete() {
        return canAutoCompete;
    }

    public void setAutoCompete(boolean autoCompete) {
        this.canAutoCompete = autoCompete;
    }

    public int getLeftOffset() {
        return mLeftOffset;
    }

    public float getTextSize() {
        return mBrush.getTextSize();
    }

    public void setTextSize(int pix) {
        if (pix <= 8 || pix >= 80 || pix == mBrush.getTextSize()) {
            return;
        }
        double oldHeight = rowHeight();
        double oldWidth = getAdvance('a');
        mZoomFactor = pix / BASE_TEXT_SIZE_PIXELS;
        mBrush.setTextSize(pix);
        mBrushLine.setTextSize(pix);
        if (mDoc.isWordWrap())
            mDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        double x = getScrollX() * ((double) getAdvance('a') / oldWidth);
        double y = getScrollY() * ((double) rowHeight() / oldHeight);
        scrollTo((int) x, (int) y);
        //log("setTextSize:"+x+","+y);
        mAlphaWidth = (int) mBrush.measureText("a");
        mSpaceWidth = (int) mBrush.measureText(" ");
        {
            invalidate();
        }
    }

    public void replaceText(int from, int charCount, String text) {
        mDoc.beginBatchEdit();
        mFieldController.replaceText(from, charCount, text);
        mFieldController.stopTextComposing();
        mDoc.endBatchEdit();
    }

    private char[] createAutoIndent() {
        int lineNum = mDoc.findLineNumber(mCaretPosition);
        int startOfLine = mDoc.getLineOffset(lineNum);
        int whitespaceCount = 0;
        mDoc.seekChar(startOfLine);
        while (mDoc.hasNext()) {
            char c = mDoc.next();
            if ((c != ' ' && c != BaseLanguage.TAB) || startOfLine + whitespaceCount >= mCaretPosition) {
                break;
            }
            ++whitespaceCount;
        }

        whitespaceCount += getLexer().autoIndent(mDoc.subSequence(startOfLine, mCaretPosition - startOfLine));
        if (whitespaceCount < 0)
            return new char[]{BaseLanguage.NEWLINE};

        char[] indent = new char[1 + whitespaceCount];
        indent[0] = BaseLanguage.NEWLINE;

        mDoc.seekChar(startOfLine);
        for (int i = 0; i < whitespaceCount; ++i) {
            indent[1 + i] = ' ';
        }
        return indent;
    }

    public void format() {
        selectText(false);
        CharSequence text = mLanguage.format(mDoc.toString());
        mDoc.beginBatchEdit();
        mDoc.deleteAt(0, mDoc.docLength() - 1, System.nanoTime());
        mDoc.insertBefore(text.toString().toCharArray(), 0, System.nanoTime());
        mDoc.endBatchEdit();
        mDoc.clearSpans();
        respan();
        invalidate();
    }

    private char[] createIndent(int n) {
        if (n < 0)
            return new char[0];
        char[] idts = new char[n];
        for (int i = 0; i < n; i++)
            idts[i] = ' ';
        return idts;
    }

    public int getLength() {
        return mDoc.docLength();
    }

    private void initView() {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        isBlockLine = defaultSharedPreferences.getBoolean("blockline", true);
        isWeekLine = defaultSharedPreferences.getBoolean("weekline", false);
        identifierColor = Color.parseColor(defaultSharedPreferences.getString("color_identifier", "#FF000000"));
        int blocklineColor = Color.parseColor(defaultSharedPreferences.getString("color_blockline", "#FFA6A6A6"));
        mFieldController = this.new TextFieldController();
        mClipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        mBrush = new Paint();
        mBrush.setAntiAlias(true);
        mBrush.setTextSize(BASE_TEXT_SIZE_PIXELS);
        mBrushLine = new Paint();
        mBrushLine.setAntiAlias(true);
        mBrushLine.setTextSize(BASE_TEXT_SIZE_PIXELS);
        mCodeLine = new Paint();
        mCodeLine.setAntiAlias(true);
        mCodeLine.setTextSize(BASE_TEXT_SIZE_PIXELS);
        mCodeLine.setColor(blocklineColor);
        mCodeLine.setStrokeWidth(2.0f);
        mCodeLine.setFakeBoldText(true);
        if (isWeekLine) {
            mCodeLine.setPathEffect(new DashPathEffect(new float[]{25, 6}, 0));
            setLayerType(LAYER_TYPE_SOFTWARE, mCodeLine);
        }
        mColorLine = new Paint();
        mColorLine.setAntiAlias(true);
        mColorLine.setColor(0xff2196f3);
        mColorLine.setStrokeWidth(6.0f);
        //setBackgroundColor(mColorScheme.getColor(Colorable.BACKGROUND));
        setLongClickable(true);
        setFocusableInTouchMode(true);
        setHapticFeedbackEnabled(true);

        mRowListener = new RowListener() {
            @Override
            public void onRowChange(int newRowIndex) {
                // Do nothing
            }
        };

        mSelModeLis = new OnSelectionChangedListener() {

            @Override
            public void onSelectionChanged(boolean active, int selStart, int selEnd) {
                if (active)
                    mClipboardPanel.show();
                else
                    mClipboardPanel.hide();
            }
        };

        mTextListener = new TextChangeListener() {
            @Override
            public void onNewLine(String c, int mCaretPosition, int p2) {
                mCaretSpan.setFirst(mCaretSpan.getFirst() + 1);
                mAutoCompletePanel.dismiss();
            }

            @Override
            public void onDel(CharSequence text, int mCaretPosition, int delCount) {
                if (delCount <= mCaretSpan.getFirst()) {
                    mCaretSpan.setFirst(mCaretSpan.getFirst() - 1);
                }
                mAutoCompletePanel.dismiss();
            }

            @Override
            public void onAdd(CharSequence text, int mCaretPosition, int addCount) {
                mCaretSpan.setFirst(mCaretSpan.getFirst() + addCount);
                if (!canAutoCompete)
                    return;
                int curr = mCaretPosition;
                for (; curr >= 0; curr--) {
                    char c = mDoc.charAt(curr - 1);
                    if (!(Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == ':')) {
                        break;
                    }
                }
                if (mCaretPosition - curr > 0)
                    mAutoCompletePanel.update(getCaretRow(), mDoc.subSequence(curr, mCaretPosition - curr));
                else
                    mAutoCompletePanel.dismiss();
            }

        };
        resetView();
        setLanguage(LanguageJava.getInstance());
        mClipboardPanel = new ClipboardPanel(this);
        mAutoCompletePanel = new JavaAutoCompletePanel(this);
        ((LanguageJava) getLanguage()).setCodePanel(mAutoCompletePanel);
        invalidate();
    }

    private void resetView() {
        mCaretPosition = 0;
        mCaretRow = 0;
        _xExtent = 0;
        mFieldController.setSelectText(false);
        mFieldController.stopTextComposing();
        mDoc.clearSpans();
        if (getContentWidth() > 0 || !mDoc.isWordWrap()) {
            mDoc.analyzeWordWrap();
        }
        mRowListener.onRowChange(0);
        scrollTo(0, 0);
    }

    /**
     * Sets the text displayed to the document referenced by hDoc. The view
     * state is reset and the view is invalidated as a side-effect.
     */
    public void setDocumentProvider(DocumentProvider hDoc) {
        mDoc = hDoc;
        resetView();
        mFieldController.cancelSpanning(); //stop existing lex threads
        mFieldController.determineSpans();
        invalidate();
    }

    /**
     * Returns a DocumentProvider that references the same Document used by the
     * FreeScrollingTextField.
     */
    public DocumentProvider createDocumentProvider() {
        return new DocumentProvider(mDoc);
    }

    public void setRowListener(RowListener rLis) {
        mRowListener = rLis;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener sLis) {
        mSelModeLis = sLis;
    }

    /**
     * Sets the caret navigation method used by this text field
     */
    public void setNavigationMethod(TouchNavigationMethod navMethod) {
        mNaviMethod = navMethod;
    }

    public void setChirality(boolean isRightHanded) {
        mNaviMethod.onChiralityChanged(isRightHanded);
    }

    // this used to be isDirty(), but was renamed to avoid conflicts with Android API 11
    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean set) {
        isEdited = set;
    }

    //---------------------------------------------------------------------
    //-------------------------- Paint methods ----------------------------

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
                | EditorInfo.IME_ACTION_DONE
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        if (mInputConnection == null) {
            mInputConnection = this.new TextFieldInputConnection(this);
        } else {
            mInputConnection.resetComposingState();
        }
        return mInputConnection;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public boolean isSaveEnabled() {
        return true;
    }

    //---------------------------------------------------------------------
    //------------------------- Layout methods ----------------------------
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(useAllDimensions(widthMeasureSpec),
                useAllDimensions(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            Rect rect = new Rect();
            getWindowVisibleDisplayFrame(rect);
            mTopOffset = rect.top + rect.height() - getHeight();
            if (!isLayout) {
                respan();
            }
            isLayout = right > 0;
            invalidate();
            mAutoCompletePanel.setWidth(getWidth() / 2);
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mDoc.isWordWrap() && oldw != w)
            mDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        if (h < oldh)
            makeCharVisible(mCaretPosition);
        if (onSizeChangedListener != null)
            onSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
    }

    /*private void doLinkLine(Canvas canvas) {
        ArrayList<LinkLine> lines = mFieldController.mLexer.getLinkLines();
        if (lines == null || lines.isEmpty())
            return;
        mColorLine.setStrokeWidth(1.6f);
        for (LinkLine line : lines) {
            if (line.type == LinkLine.TYPE_STRING) {
                mColorLine.setColor(getColorScheme().getColor(Colorable.STRING));
            } else {
                mColorLine.setColor(getColorScheme().getColor(Colorable.COMMENT));
            }
            canvas.drawLine(getCharExtent(line.startColumn).getFirst(),
                    (line.line + 1) * rowHeight() - 2,
                    getCharExtent(line.endColumn).getFirst(),
                    (line.line + 1) * rowHeight() - 2, mColorLine);
        }
    }*/

    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        this.onSizeChangedListener = listener;
    }

    private int useAllDimensions(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int result = MeasureSpec.getSize(measureSpec);

        if (specMode != MeasureSpec.EXACTLY && specMode != MeasureSpec.AT_MOST) {
            result = Integer.MAX_VALUE;
            TextWarriorException.fail("MeasureSpec cannot be UNSPECIFIED. Setting dimensions to max.");
        }

        return result;
    }

    protected int getNumVisibleRows() {
        return (int) Math.ceil((double) getContentHeight() / rowHeight());
    }

    public int rowHeight() {
        Paint.FontMetricsInt metrics = mBrush.getFontMetricsInt();
        return (metrics.descent - metrics.ascent);
    }

    /*
	 The only methods that have to worry about padding are invalidate, draw
	 and computeVerticalScrollRange() methods. Other methods can assume that
	 the text completely fills a rectangular viewport given by getContentWidth()
	 and getContentHeight()
	 */
    protected int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    protected int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Determines if the View has been layout or is still being constructed
     */
    public boolean hasLayout() {
        return (getWidth() == 0); // simplistic implementation, but should work for most cases
    }

    /**
     * The first row of text to paint, which may be partially visible.
     * Deduced from the clipping rectangle given to onDraw()
     */
    private int getBeginPaintRow(Canvas canvas) {
        Rect bounds = canvas.getClipBounds();
        return bounds.top / rowHeight();
    }

    /**
     * The last row of text to paint, which may be partially visible.
     * Deduced from the clipping rectangle given to onDraw()
     */
    private int getEndPaintRow(Canvas canvas) {
        //clip top and left are inclusive; bottom and right are exclusive
        Rect bounds = canvas.getClipBounds();
        return (bounds.bottom - 1) / rowHeight();
    }

    /**
     * @return The x-value of the baseline for drawing text on the given row
     */
    public int getPaintBaseline(int row) {
        Paint.FontMetricsInt metrics = mBrush.getFontMetricsInt();
        return (row + 1) * rowHeight() - metrics.descent;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();

        //translate clipping region to create padding around edges
        canvas.clipRect(getScrollX() + getPaddingLeft(),
                getScrollY() + getPaddingTop(),
                getScrollX() + getWidth() - getPaddingRight(),
                getScrollY() + getHeight() - getPaddingBottom());
        canvas.translate(getPaddingLeft(), getPaddingTop());
        //log("x:"+getScrollX()+",y:"+getScrollY()+"{left}"+getPaddingLeft()+"{top}"+getPaddingTop());
        realDraw(canvas);
        canvas.restore();
        mNaviMethod.onTextDrawComplete(canvas);
    }

    /**
     * 绘制
     *
     * @param canvas
     */
    private void realDraw(Canvas canvas) {
        //log("mLeftOffset:"+mLeftOffset);
        int beginPaintRow = getBeginPaintRow(canvas);
        int currentIndex = mDoc.getLineOffset(beginPaintRow);

        if (currentIndex < 0) {
            return;
        }
        int currRowNum = getBeginPaintRow(canvas);
        int currLineNum = isWordWrap() ? mDoc.findLineNumber(currentIndex) + 1 : currRowNum + 1;
        int lastLineNum = 0;
        if (mShowLineNumbers) {
            mLeftOffset = (int) mBrushLine.measureText(" " + mDoc.getRowCount());
        } else {
            mLeftOffset = 0;
        }
        //----------------------------------------------
        // set up span coloring settings
        //----------------------------------------------
        int spanIndex = 0;
        //得到一个词法分析的结果
        List<Pair> spans = mDoc.getSpans();

        // There must be at least one span to paint, even for an empty file,
        // where the span contains only the EOF character
        TextWarriorException.assertVerbose(!spans.isEmpty(),
                "No spans to paint in TextWarrior.paint()");

        Pair nextSpan = (Pair) spans.get(spanIndex++);
        Pair currSpan;
        do {
            currSpan = nextSpan;
            if (spanIndex < spans.size()) {
                nextSpan = (Pair) spans.get(spanIndex++);
            } else {
                nextSpan = null;
            }
        }
        while (nextSpan != null &&
                nextSpan.getFirst() <= currentIndex);

        int spanColor = mColorScheme.getTokenColor(currSpan.getSecond());
        mBrush.setColor(spanColor);

        //----------------------------------------------
        // set up graphics settings
        //----------------------------------------------
        int paintX;
        int paintY = getPaintBaseline(beginPaintRow);
        int endY = getPaintBaseline(getEndPaintRow(canvas));

        //----------------------------------------------
        // start painting!
        //----------------------------------------------
        canvas.drawColor(mColorScheme.getColor(Colorable.BACKGROUND));
        int rowCount = mDoc.getRowCount();
        if (mShowLineNumbers) {
            mBrushLine.setColor(getColorScheme().getColor(Colorable.NON_PRINTING_GLYPH));
            mBrushLine.setStrokeWidth(3.0f);
            //mBrushLine.setFakeBoldText(true);
            //log("mLeftOffset:"+mLeftOffset);
            canvas.drawLine(mLeftOffset - mSpaceWidth / 2, getScrollY(), mLeftOffset - mSpaceWidth / 2, getScrollY() + getHeight(), mBrushLine);
        }
        mDoc.seekChar(currentIndex);//从currentIndex开始迭代
        paintX = mLeftOffset;


        while (paintY <= endY && mDoc.hasNext()) {

            if (currRowNum > rowCount) {
                break;
            }

            if (mShowLineNumbers && currLineNum != lastLineNum) {
                lastLineNum = currLineNum;
                String num = String.valueOf(currLineNum);
//				int len=num.length();
//				int numX=(int)((maxTextLen-len)*getTextSize());


//				log("len:"+numX+","+getTextSize());
                drawLineNum(canvas, num, 0, paintY);
            }

            // check if formatting changes are needed
            if (reachedNextSpan(currentIndex, nextSpan)) {
                currSpan = nextSpan;
                spanColor = mColorScheme.getTokenColor(currSpan.getSecond());
                mBrush.setColor(spanColor);

                if (spanIndex < spans.size()) {
                    nextSpan = (Pair) spans.get(spanIndex++);
                } else {
                    nextSpan = null;
                }
            }
            if (currentIndex == mCaretPosition) {
                drawCaret(canvas, paintX, paintY);

            } else if (currentIndex + 1 == mCaretPosition) {
                mCaretSpan = currSpan;
            }
            char c = mDoc.next();
            if (mFieldController.inSelectionRange(currentIndex)) {
                paintX += drawSelectedText(canvas, c, paintX, paintY);
            } else {
                paintX += drawChar(canvas, c, paintX, paintY);
            }

            ++currentIndex;
            if (c == BaseLanguage.NEWLINE) {
                paintY += rowHeight();
                if (paintX >= _xExtent) {
                    _xExtent = paintX;
                }
                paintX = mLeftOffset;
                currLineNum++;
            }
        }
        doOptionHighlightRow(canvas);
        if (!isWordWrap()) {
            doColorBlock(canvas);
            //doDeviderLine(canvas);
            //doLinkLine(canvas);
            if (isBlockLine)
                doBlockLine(canvas);
        }
    }

    private void doDeviderLine(Canvas canvas) {
        ArrayList<DeviderLine> lines = mFieldController.mLexer.getDeviderLines();
        if (lines == null || lines.isEmpty())
            return;
        mColorLine.setColor(identifierColor);
        mColorLine.setStrokeWidth(2.5f);
        for (DeviderLine line : lines) {
            canvas.drawLine(rowHeight() * 2,
                    (line.line + 1) * rowHeight(),
                    rowHeight() * 10,
                    (line.line + 1) * rowHeight(), mColorLine);
        }
    }

    private void doColorBlock(Canvas canvas) {
        ArrayList<ColorLine> lines = mFieldController.mLexer.getColorLines();
        if (lines == null || lines.isEmpty())
            return;
        mColorLine.setStrokeWidth(6.0f);
        for (ColorLine line : lines) {
            mColorLine.setColor(line.color);
            canvas.drawLine(getCharExtent(line.startColumn).getFirst(),
                    (line.line + 1) * rowHeight(),
                    getCharExtent(line.endColumn).getFirst(),
                    (line.line + 1) * rowHeight(), mColorLine);
        }
    }

    private void doBlockLine(Canvas canvas) {
        ArrayList<BlockLine> lines = mFieldController.mLexer.getLines();
        if (lines == null || lines.isEmpty())
            return;
        Rect bounds = canvas.getClipBounds();
        int bt = bounds.top;
        int bb = bounds.bottom;
        for (BlockLine line : lines) {
            if (line.startLine == mCaretRow) {
                doBlockRow(canvas, line.endLine);
            } else if (line.endLine == mCaretRow) {
                doBlockRow(canvas, line.startLine);
            }
            int top = (line.startLine + 1) * rowHeight();
            int bottom = line.endLine * rowHeight();
            if (bottom < bt || top > bb)
                continue;
            int left = Math.min(getCharExtent(line.startColumn).getFirst(),
                    getCharExtent(line.endColumn).getFirst()) + 5;
            if (!isWeekLine) {
                mCodeLine.setPathEffect(null);
            }
            if (line.type == BlockLine.TYPE_NORMAL) {
                canvas.drawLine(left, top, left, bottom, mCodeLine);
            } else {
                //向左边画
                int startX = getCharExtent(line.startColumn).getFirst();
                int startY = top - rowHeight() / 2;
                int endX = (int) (startX - rowHeight() / 1.5);
                int endY = startY;
                canvas.drawLine(startX, startY, endX, endY, mCodeLine);
                //画竖线
                int mBottom = bottom + rowHeight() / 2;
                canvas.drawLine(endX, endY, endX, mBottom, mCodeLine);
                //向右画
                canvas.drawLine(endX, mBottom, startX, mBottom, mCodeLine);
                //画箭头
                if (line.type == BlockLine.TYPE_FLOW_DOWN) {
                    drawArrow(canvas, mCodeLine,
                            startX - rowHeight() / 4, mBottom - rowHeight() / 4,
                            startX - rowHeight() / 4, mBottom + rowHeight() / 4,
                            startX, mBottom);
                } else {
                    drawArrow(canvas, mCodeLine,
                            startX - rowHeight() / 4, startY - rowHeight() / 4,
                            startX - rowHeight() / 4, startY + rowHeight() / 4,
                            startX, startY);
                    //画向下指箭头的虚线
                    mCodeLine.setPathEffect(new DashPathEffect(new float[]{25, 6}, 0));
                    setLayerType(LAYER_TYPE_SOFTWARE, mCodeLine);
                    canvas.drawLine(left, top, left, bottom, mCodeLine);
                    drawArrow(canvas, mCodeLine,
                            left - rowHeight() / 4, bottom - rowHeight() / 4,
                            left + rowHeight() / 4, bottom - rowHeight() / 4,
                            left, bottom);
                }
            }
        }
    }

    //画箭头
    public void drawArrow(Canvas canvas, Paint paint, int x1, int y1, int x2, int y2, int x3, int y3) {
        Path triangle = new Path();
        triangle.moveTo(x1, y1);
        triangle.lineTo(x2, y2);
        triangle.lineTo(x3, y3);
        triangle.close();
        canvas.drawPath(triangle, paint);
    }

    private void doBlockRow(Canvas canvas, int mCaretRow) {
        if (isHighlightRow) {
            int y = getPaintBaseline(mCaretRow);
            int originalColor = mBrush.getColor();
            mBrush.setColor(mColorScheme.getColor(Colorable.LINE_HIGHLIGHT));

            int lineLength = Math.max(_xExtent, getContentWidth());
            //canvas.drawRect(0, y+1, lineLength, y+2, mBrush);
            drawTextBackground(canvas, 0, y, lineLength);
            //mBrush.setColor(0x88000000);
            mBrush.setColor(originalColor);
        }
    }

    /**
     * Underline the caret row if the option for highlighting it is set
     */
    private void doOptionHighlightRow(Canvas canvas) {
        if (isHighlightRow) {
            int y = getPaintBaseline(mCaretRow);
            int originalColor = mBrush.getColor();
            mBrush.setColor(mColorScheme.getColor(Colorable.LINE_HIGHLIGHT));

            int lineLength = Math.max(_xExtent, getContentWidth());
            //canvas.drawRect(0, y+1, lineLength, y+2, mBrush);
            drawTextBackground(canvas, 0, mCaretY, lineLength);
            mBrush.setColor(0x88000000);
            mBrush.setColor(originalColor);
        }
    }

    private int drawChar(Canvas canvas, char c, int paintX, int paintY) {
        int originalColor = mBrush.getColor();
        int charWidth = getAdvance(c, paintX);

        if (paintX > getScrollX() || paintX < (getScrollX() + getContentWidth()))
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    mEmoji = c;
                    break;
                case ' ':
                    if (mShowNonPrinting) {
                        mBrush.setColor(mColorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
                        canvas.drawText(BaseLanguage.GLYPH_SPACE, 0, 1, paintX, paintY, mBrush);
                        mBrush.setColor(originalColor);
                    } else {
                        canvas.drawText(" ", 0, 1, paintX, paintY, mBrush);
                    }
                    break;

                case BaseLanguage.EOF: //fall-through
                case BaseLanguage.NEWLINE:
                    if (mShowNonPrinting) {
                        mBrush.setColor(mColorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
                        canvas.drawText(BaseLanguage.GLYPH_NEWLINE, 0, 1, paintX, paintY, mBrush);
                        mBrush.setColor(originalColor);
                    }
                    break;

                case BaseLanguage.TAB:
                    if (mShowNonPrinting) {
                        mBrush.setColor(mColorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
                        canvas.drawText(BaseLanguage.GLYPH_TAB, 0, 1, paintX, paintY, mBrush);
                        mBrush.setColor(originalColor);
                    }
                    break;

                default:
                    if (mEmoji != 0) {
                        canvas.drawText(new char[]{mEmoji, c}, 0, 2, paintX, paintY, mBrush);
                        mEmoji = 0;
                    } else {
                        char[] ca = {c};
                        canvas.drawText(ca, 0, 1, paintX, paintY, mBrush);
                    }
                    break;
            }

        return charWidth;
    }

    // paintY is the baseline for text, NOT the top extent
    private void drawTextBackground(Canvas canvas, int paintX, int paintY,
                                    int advance) {
        Paint.FontMetricsInt metrics = mBrush.getFontMetricsInt();
        canvas.drawRect(paintX,
                paintY + metrics.ascent,
                paintX + advance,
                paintY + metrics.descent,
                mBrush);
    }

    private int drawSelectedText(Canvas canvas, char c, int paintX, int paintY) {
        int oldColor = mBrush.getColor();
        int advance = getAdvance(c);

        mBrush.setColor(mColorScheme.getColor(Colorable.SELECTION_BACKGROUND));
        drawTextBackground(canvas, paintX, paintY, advance);

        mBrush.setColor(mColorScheme.getColor(Colorable.SELECTION_FOREGROUND));
        drawChar(canvas, c, paintX, paintY);

        mBrush.setColor(oldColor);
        return advance;
    }

    private void drawCaret(Canvas canvas, int paintX, int paintY) {
        int originalColor = mBrush.getColor();
        mCaretX = paintX;
        mCaretY = paintY;

        int caretColor = mColorScheme.getColor(Colorable.CARET_DISABLED);
        mBrush.setColor(caretColor);
        // draw full caret
        drawTextBackground(canvas, paintX - 1, paintY, 2);
        mBrush.setColor(originalColor);
    }

    private int drawLineNum(Canvas canvas, String s, int paintX, int paintY) {
        //int originalColor = mBrush.getColor();
        //mBrush.setColor(mColorScheme.getColor(Colorable.NON_PRINTING_GLYPH));

        canvas.drawText(s, paintX, paintY, mBrushLine);
        //mBrush.setColor(originalColor);
        return 0;
    }

    @Override
    final public int getRowWidth() {
        return getContentWidth() - mLeftOffset;
    }

    /**
     * Returns printed width of c.
     * <p>
     * Takes into account user-specified tab width and also handles
     * application-defined widths for NEWLINE and EOF
     *
     * @param c Character to measure
     * @return Advance of character, in pixels
     */
    @Override
    public int getAdvance(char c) {
        int advance;

        switch (c) {
            case 0xd83c:
            case 0xd83d:
                advance = 0;
                break;
            case ' ':
                advance = getSpaceAdvance();
                break;
            case BaseLanguage.NEWLINE: // fall-through
            case BaseLanguage.EOF:
                advance = getEOLAdvance();
                break;
            case BaseLanguage.TAB:
                advance = getTabAdvance();
                break;
            default:
                if (mEmoji != 0) {
                    char[] ca = {mEmoji, c};
                    advance = (int) mBrush.measureText(ca, 0, 2);
                } else {
                    char[] ca = {c};
                    advance = (int) mBrush.measureText(ca, 0, 1);
                }
                break;
        }

        return advance;
    }

    public int getAdvance(char c, int x) {
        int advance;

        switch (c) {
            case 0xd83c:
            case 0xd83d:
                advance = 0;
                break;
            case ' ':
                advance = getSpaceAdvance();
                break;
            case BaseLanguage.NEWLINE: // fall-through
            case BaseLanguage.EOF:
                advance = getEOLAdvance();
                break;
            case BaseLanguage.TAB:
                advance = getTabAdvance(x);
                break;
            default:
                if (mEmoji != 0) {
                    char[] ca = {mEmoji, c};
                    advance = (int) mBrush.measureText(ca, 0, 2);
                } else {
                    char[] ca = {c};
                    advance = (int) mBrush.measureText(ca, 0, 1);
                }
                break;
        }

        return advance;
    }

    public int getCharAdvance(char c) {
        int advance;
        char[] ca = {c};
        advance = (int) mBrush.measureText(ca, 0, 1);
        return advance;
    }

    protected int getSpaceAdvance() {
        if (mShowNonPrinting) {
            return (int) mBrush.measureText(BaseLanguage.GLYPH_SPACE,
                    0, BaseLanguage.GLYPH_SPACE.length());
        } else {
            return mSpaceWidth;
        }
    }


    //---------------------------------------------------------------------
    //------------------- Scrolling and touch -----------------------------

    protected int getEOLAdvance() {
        if (mShowNonPrinting) {
            return (int) mBrush.measureText(BaseLanguage.GLYPH_NEWLINE,
                    0, BaseLanguage.GLYPH_NEWLINE.length());
        } else {
            return (int) (EMPTY_CARET_WIDTH_SCALE * mBrush.measureText(" ", 0, 1));
        }
    }

    protected int getTabAdvance() {
        if (mShowNonPrinting) {
            return mTabLength * (int) mBrush.measureText(BaseLanguage.GLYPH_SPACE,
                    0, BaseLanguage.GLYPH_SPACE.length());
        } else {
            return mTabLength * mSpaceWidth;
        }
    }

    protected int getTabAdvance(int x) {
        if (mShowNonPrinting) {
            return mTabLength * (int) mBrush.measureText(BaseLanguage.GLYPH_SPACE,
                    0, BaseLanguage.GLYPH_SPACE.length());
        } else {
            int i = (x - mLeftOffset) / mSpaceWidth % mTabLength;
            return (mTabLength - i) * mSpaceWidth;
        }
    }

    /**
     * Invalidate rows from startRow (inclusive) to endRow (exclusive)
     */
    private void invalidateRows(int startRow, int endRow) {
        TextWarriorException.assertVerbose(startRow <= endRow && startRow >= 0,
                "Invalid startRow and/or endRow");

        Rect caretSpill = mNaviMethod.getCaretBloat();
        // that rows have to be invalidated as well.
        // This is a problem for Thai, Vietnamese and Indic scripts
        Paint.FontMetricsInt metrics = mBrush.getFontMetricsInt();
        int top = startRow * rowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                endRow * rowHeight() + getPaddingTop() + caretSpill.bottom);
    }

    /**
     * Invalidate rows from startRow (inclusive) to the end of the field
     */
    private void invalidateFromRow(int startRow) {
        TextWarriorException.assertVerbose(startRow >= 0,
                "Invalid startRow");

        Rect caretSpill = mNaviMethod.getCaretBloat();
        // that rows have to be invalidated as well.
        // This is a problem for Thai, Vietnamese and Indic scripts
        Paint.FontMetricsInt metrics = mBrush.getFontMetricsInt();
        int top = startRow * rowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                getScrollY() + getHeight());
    }

    private void invalidateCaretRow() {
        invalidateRows(mCaretRow, mCaretRow + 1);
    }

    private void invalidateSelectionRows() {
        int startRow = mDoc.findRowNumber(mSelectionAnchor);
        int endRow = mDoc.findRowNumber(mSelectionEdge);

        invalidateRows(startRow, endRow + 1);
    }

    /**
     * Scrolls the text horizontally and/or vertically if the character
     * specified by charOffset is not in the visible text region.
     * The view is invalidated if it is scrolled.
     *
     * @param charOffset The index of the character to make visible
     * @return True if the drawing area was scrolled horizontally
     * and/or vertically
     */
    private boolean makeCharVisible(int charOffset) {
        TextWarriorException.assertVerbose(
                charOffset >= 0 && charOffset < mDoc.docLength(),
                "Invalid charOffset given");
        int scrollVerticalBy = makeCharRowVisible(charOffset);
        int scrollHorizontalBy = makeCharColumnVisible(charOffset);

        if (scrollVerticalBy == 0 && scrollHorizontalBy == 0) {
            return false;
        } else {
            if (charOffset != 0) {
                scrollBy(scrollHorizontalBy, scrollVerticalBy);
            }
            //log("makeCharVisible:"+scrollHorizontalBy+","+getScrollX()+","+charOffset);
            return true;
        }
    }

    /**
     * Calculates the amount to scroll vertically if the char is not
     * in the visible region.
     *
     * @param charOffset The index of the character to make visible
     * @return The amount to scroll vertically
     */
    private int makeCharRowVisible(int charOffset) {
        int scrollBy = 0;
        int charTop = mDoc.findRowNumber(charOffset) * rowHeight();
        int charBottom = charTop + rowHeight();

        if (charTop < getScrollY()) {
            scrollBy = charTop - getScrollY();
        } else if (charBottom > (getScrollY() + getContentHeight())) {
            scrollBy = charBottom - getScrollY() - getContentHeight();
        }

        return scrollBy;
    }

    /**
     * Calculates the amount to scroll horizontally if the char is not
     * in the visible region.
     *
     * @param charOffset The index of the character to make visible
     * @return The amount to scroll horizontally
     */
    private int makeCharColumnVisible(int charOffset) {
        int scrollBy = 0;
        Pair visibleRange = getCharExtent(charOffset);

        int charLeft = visibleRange.getFirst();
        int charRight = visibleRange.getSecond();

        if (charRight > (getScrollX() + getContentWidth())) {
            scrollBy = charRight - getScrollX() - getContentWidth();
        }

        if (charLeft < getScrollX() + mAlphaWidth) {
            scrollBy = charLeft - getScrollX() - mAlphaWidth;
        }

        return scrollBy;
    }

    /**
     * Calculates the x-coordinate extent of charOffset.
     *
     * @return The x-values of left and right edges of charOffset. Pair.first
     * contains the left edge and Pair.second contains the right edge
     */
    protected Pair getCharExtent(int charOffset) {
        int row = mDoc.findRowNumber(charOffset);
        int rowOffset = mDoc.getRowOffset(row);
        int left = mLeftOffset;
        int right = mLeftOffset;
        boolean isEmoji = false;
        String rowText = mDoc.getRow(row);
        int i = 0;

        int len = rowText.length();
        while (rowOffset + i <= charOffset && i < len) {
            char c = rowText.charAt(i);
            left = right;
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    isEmoji = true;
                    char[] ca = {c, rowText.charAt(i + 1)};
                    right += (int) mBrush.measureText(ca, 0, 2);
                    break;
                case BaseLanguage.NEWLINE:
                case BaseLanguage.EOF:
                    right += getEOLAdvance();
                    break;
                case ' ':
                    right += getSpaceAdvance();
                    break;
                case BaseLanguage.TAB:
                    right += getTabAdvance(right);
                    break;
                default:
                    if (isEmoji)
                        isEmoji = false;
                    else
                        right += getCharAdvance(c);
                    break;
            }
            ++i;
        }
        return new Pair(left, right);
    }

    /**
     * Returns the bounding box of a character in the text field.
     * The coordinate system used is one where (0, 0) is the top left corner
     * of the text, before padding is added.
     *
     * @param charOffset The character offset of the character of interest
     * @return Rect(left, top, right, bottom) of the character bounds,
     * or Rect(-1, -1, -1, -1) if there is no character at that coordinate.
     */
    Rect getBoundingBox(int charOffset) {
        if (charOffset < 0 || charOffset >= mDoc.docLength()) {
            return new Rect(-1, -1, -1, -1);
        }

        int row = mDoc.findRowNumber(charOffset);
        int top = row * rowHeight();
        int bottom = top + rowHeight();

        Pair xExtent = getCharExtent(charOffset);
        int left = xExtent.getFirst();
        int right = xExtent.getSecond();

        return new Rect(left, top, right, bottom);
    }

    public ColorScheme getColorScheme() {
        return mColorScheme;
    }

    public void setColorScheme(ColorScheme colorScheme) {
        mColorScheme = colorScheme;
        mNaviMethod.onColorSchemeChanged(colorScheme);
        setBackgroundColor(colorScheme.getColor(Colorable.BACKGROUND));
    }

    /**
     * Maps a coordinate to the character that it is on. If the coordinate is
     * on empty space, the nearest character on the corresponding row is returned.
     * If there is no character on the row, -1 is returned.
     * <p>
     * The coordinates passed in should not have padding applied to them.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return The index of the closest character, or -1 if there is
     * no character or nearest character at that coordinate
     */
    int coordToCharIndex(int x, int y) {
        int row = y / rowHeight();
        if (row > mDoc.getRowCount())
            return mDoc.docLength() - 1;

        int charIndex = mDoc.getRowOffset(row);
        if (charIndex < 0) {
            //non-existent row
            return -1;
        }

        if (x < 0) {
            return charIndex; // coordinate is outside, to the left of view
        }

        String rowText = mDoc.getRow(row);

        int extent = mLeftOffset;
        int i = 0;
        boolean isEmoji = false;

        //x-=getAdvance('a')/2;
        int len = rowText.length();
        while (i < len) {
            char c = rowText.charAt(i);
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    isEmoji = true;
                    char[] ca = {c, rowText.charAt(i + 1)};
                    extent += (int) mBrush.measureText(ca, 0, 2);
                    break;
                case BaseLanguage.NEWLINE:
                case BaseLanguage.EOF:
                    extent += getEOLAdvance();
                    break;
                case ' ':
                    extent += getSpaceAdvance();
                    break;
                case BaseLanguage.TAB:
                    extent += getTabAdvance(extent);
                    break;
                default:
                    if (isEmoji)
                        isEmoji = false;
                    else
                        extent += getCharAdvance(c);

            }

            if (extent >= x) {
                break;
            }

            ++i;
        }


        if (i < rowText.length()) {
            return charIndex + i;
        }
        //nearest char is last char of line
        return charIndex + i - 1;
    }

    /**
     * Maps a coordinate to the character that it is on.
     * Returns -1 if there is no character on the coordinate.
     * <p>
     * The coordinates passed in should not have padding applied to them.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return The index of the character that is on the coordinate,
     * or -1 if there is no character at that coordinate.
     */
    int coordToCharIndexStrict(int x, int y) {
        int row = y / rowHeight();
        int charIndex = mDoc.getRowOffset(row);

        if (charIndex < 0 || x < 0) {
            //non-existent row
            return -1;
        }

        String rowText = mDoc.getRow(row);

        int extent = 0;
        int i = 0;
        boolean isEmoji = false;

        //x-=getAdvance('a')/2;
        int len = rowText.length();
        while (i < len) {
            char c = rowText.charAt(i);
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    isEmoji = true;
                    char[] ca = {c, rowText.charAt(i + 1)};
                    extent += (int) mBrush.measureText(ca, 0, 2);
                    break;
                case BaseLanguage.NEWLINE:
                case BaseLanguage.EOF:
                    extent += getEOLAdvance();
                    break;
                case ' ':
                    extent += getSpaceAdvance();
                    break;
                case BaseLanguage.TAB:
                    extent += getTabAdvance(extent);
                    break;
                default:
                    if (isEmoji)
                        isEmoji = false;
                    else
                        extent += getCharAdvance(c);

            }

            if (extent >= x) {
                break;
            }

            ++i;
        }

        if (i < rowText.length()) {
            return charIndex + i;
        }

        //no char enclosing x
        return -1;
    }

    /**
     * Not private to allow access by TouchNavigationMethod
     *
     * @return The maximum x-value that can be scrolled to for the current rows
     * of text in the viewport.
     */
    int getMaxScrollX() {
        if (isWordWrap())
            return mLeftOffset;
        else
            return Math.max(0,
                    _xExtent - getContentWidth() + mNaviMethod.getCaretBloat().right + mAlphaWidth);
    }

    /**
     * Not private to allow access by TouchNavigationMethod
     *
     * @return The maximum y-value that can be scrolled to.
     */
    int getMaxScrollY() {
        return Math.max(0,
                mDoc.getRowCount() * rowHeight() - getContentHeight() / 2 + mNaviMethod.getCaretBloat().bottom);
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return getScrollY();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mDoc.getRowCount() * rowHeight() + getPaddingTop() + getPaddingBottom();
    }

    @Override
    public void computeScroll() {

        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    public final void smoothScrollBy(int dx, int dy) {
        if (getHeight() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > 250) {
            //final int maxY = getMaxScrollX();
            final int scrollY = getScrollY();
            final int scrollX = getScrollX();

            //dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;

            mScroller.startScroll(scrollX, scrollY, dx, dy);
            postInvalidate();
        } else {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    /**
     * Start fling scrolling
     */
    void flingScroll(int velocityX, int velocityY) {

        mScroller.fling(getScrollX(), getScrollY(), velocityX, velocityY,
                0, getMaxScrollX(), 0, getMaxScrollY());
        // Keep on drawing until the animation has finished.
        postInvalidate();
        //postInvalidateOnAnimation();
    }

    public boolean isFlingScrolling() {
        return !mScroller.isFinished();
    }


    //---------------------------------------------------------------------
    //------------------------- Caret methods -----------------------------

    public void stopFlingScrolling() {
        mScroller.forceFinished(true);
    }

    /**
     * Starting scrolling continuously in scrollDir.
     * Not private to allow access by TouchNavigationMethod.
     *
     * @return True if auto-scrolling started
     */
    boolean autoScrollCaret(int scrollDir) {
        boolean scrolled = false;
        switch (scrollDir) {
            case SCROLL_UP:
                removeCallbacks(_scrollCaretUpTask);
                if ((!caretOnFirstRowOfFile())) {
                    post(_scrollCaretUpTask);
                    scrolled = true;
                }
                break;
            case SCROLL_DOWN:
                removeCallbacks(_scrollCaretDownTask);
                if (!caretOnLastRowOfFile()) {
                    post(_scrollCaretDownTask);
                    scrolled = true;
                }
                break;
            case SCROLL_LEFT:
                removeCallbacks(_scrollCaretLeftTask);
                if (mCaretPosition > 0 &&
                        mCaretRow == mDoc.findRowNumber(mCaretPosition - 1)) {
                    post(_scrollCaretLeftTask);
                    scrolled = true;
                }
                break;
            case SCROLL_RIGHT:
                removeCallbacks(_scrollCaretRightTask);
                if (!caretOnEOF() &&
                        mCaretRow == mDoc.findRowNumber(mCaretPosition + 1)) {
                    post(_scrollCaretRightTask);
                    scrolled = true;
                }
                break;
            default:
                TextWarriorException.fail("Invalid scroll direction");
                break;
        }
        return scrolled;
    }

    /**
     * Stops automatic scrolling initiated by autoScrollCaret(int).
     * Not private to allow access by TouchNavigationMethod
     */
    void stopAutoScrollCaret() {
        removeCallbacks(_scrollCaretDownTask);
        removeCallbacks(_scrollCaretUpTask);
        removeCallbacks(_scrollCaretLeftTask);
        removeCallbacks(_scrollCaretRightTask);
    }

    /**
     * Stops automatic scrolling in scrollDir direction.
     * Not private to allow access by TouchNavigationMethod
     */
    void stopAutoScrollCaret(int scrollDir) {
        switch (scrollDir) {
            case SCROLL_UP:
                removeCallbacks(_scrollCaretUpTask);
                break;
            case SCROLL_DOWN:
                removeCallbacks(_scrollCaretDownTask);
                break;
            case SCROLL_LEFT:
                removeCallbacks(_scrollCaretLeftTask);
                break;
            case SCROLL_RIGHT:
                removeCallbacks(_scrollCaretRightTask);
                break;
            default:
                TextWarriorException.fail("Invalid scroll direction");
                break;
        }
    }

    public int getCaretRow() {
        return mCaretRow;
    }

    public int getCaretPosition() {
        return mCaretPosition;
    }

    /**
     * Sets the caret to position i, scrolls it to view and invalidates
     * the necessary areas for redrawing
     *
     * @param i The character index that the caret should be set to
     */
    public void moveCaret(int i) {
        mFieldController.moveCaret(i);
    }

    /**
     * Sets the caret one position ic_back, scrolls it on screen, and invalidates
     * the necessary areas for redrawing.
     * <p>
     * If the caret is already on the first character, nothing will happen.
     */
    public void moveCaretLeft() {
        mFieldController.moveCaretLeft(false);
    }

    /**
     * Sets the caret one position forward, scrolls it on screen, and
     * invalidates the necessary areas for redrawing.
     * <p>
     * If the caret is already on the last character, nothing will happen.
     */
    public void moveCaretRight() {
        mFieldController.moveCaretRight(false);
    }

    /**
     * Sets the caret one row down, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p>
     * If the caret is already on the last row, nothing will happen.
     */
    public void moveCaretDown() {
        mFieldController.moveCaretDown();
    }

    /**
     * Sets the caret one row up, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p>
     * If the caret is already on the first row, nothing will happen.
     */
    public void moveCaretUp() {
        mFieldController.moveCaretUp();
    }

    /**
     * Scrolls the caret into view if it is not on screen
     */
    public void focusCaret() {
        makeCharVisible(mCaretPosition);
    }


    //---------------------------------------------------------------------
    //------------------------- Text Selection ----------------------------

    /**
     * @return The column number where charOffset appears on
     */
    public int getColumn(int charOffset) {
        int row = mDoc.findRowNumber(charOffset);
        TextWarriorException.assertVerbose(row >= 0,
                "Invalid char offset given to getColumn");
        int firstCharOfRow = mDoc.getRowOffset(row);
        return charOffset - firstCharOfRow;
    }

    protected boolean caretOnFirstRowOfFile() {
        return (mCaretRow == 0);
    }

    protected boolean caretOnLastRowOfFile() {
        return (mCaretRow == (mDoc.getRowCount() - 1));
    }

    protected boolean caretOnEOF() {
        return (mCaretPosition == (mDoc.docLength() - 1));
    }

    public final boolean isSelectText() {
        return mFieldController.isSelectText();
    }

    public final boolean isSelectText2() {
        return mFieldController.isSelectText2();
    }

    /**
     * Enter or exit select mode.
     * Invalidates necessary areas for repainting.
     *
     * @param mode If true, enter select mode; else exit select mode
     */
    public void selectText(boolean mode) {
        if (mFieldController.isSelectText() && !mode) {
            invalidateSelectionRows();
            mFieldController.setSelectText(false);
        } else if (!mFieldController.isSelectText() && mode) {
            invalidateCaretRow();
            mFieldController.setSelectText(true);
        }
    }

    public void selectAll() {
        mFieldController.setSelectionRange(0, mDoc.docLength() - 1, false, true);
    }

    public void setSelection(int beginPosition, int numChars) {
        mFieldController.setSelectionRange(beginPosition, numChars, true, false);
    }

    public void setSelectionRange(int beginPosition, int numChars) {
        mFieldController.setSelectionRange(beginPosition, numChars, true, true);
    }

    public boolean inSelectionRange(int charOffset) {
        return mFieldController.inSelectionRange(charOffset);
    }

    public int getSelectionStart() {
        if (mSelectionAnchor < 0)
            return mCaretPosition;
        else
            return mSelectionAnchor;
    }

    public int getSelectionEnd() {
        if (mSelectionEdge < 0)
            return mCaretPosition;
        else
            return mSelectionEdge;
    }

    public void focusSelectionStart() {
        mFieldController.focusSelection(true);
    }

    public void focusSelectionEnd() {
        mFieldController.focusSelection(false);
    }

    public void cut() {
        if (mSelectionAnchor != mSelectionEdge)
            mFieldController.cut(mClipboardManager);
    }

    public void copy() {
        if (mSelectionAnchor != mSelectionEdge)
            mFieldController.copy(mClipboardManager);
        selectText(false);
    }

    //---------------------------------------------------------------------
    //------------------------- Formatting methods ------------------------

    public void paste() {
        CharSequence text = mClipboardManager.getText();
        if (text != null)
            mFieldController.paste(text.toString());
    }

    public void cut(ClipboardManager cb) {
        mFieldController.cut(cb);
    }

    public void copy(ClipboardManager cb) {
        mFieldController.copy(cb);
    }

    public void paste(String text) {
        mFieldController.paste(text);
    }

    private boolean reachedNextSpan(int charIndex, Pair span) {
        return (span != null) && (charIndex == span.getFirst());
    }

    public void respan() {
        mFieldController.determineSpans();
    }

    public void cancelSpanning() {
        mFieldController.cancelSpanning();
    }

    /**
     * Sets the text to use the new typeface, scrolls the view to display the
     * caret if needed, and invalidates the entire view
     */
    public void setTypeface(Typeface typeface) {
        mBrush.setTypeface(typeface);
        mBrushLine.setTypeface(typeface);
        if (mDoc.isWordWrap())
            mDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        if (!makeCharVisible(mCaretPosition)) {
            invalidate();
        }
    }

    public boolean isWordWrap() {
        return mDoc.isWordWrap();
    }

    public void setWordWrap(boolean enable) {
        mDoc.setWordWrap(enable);

        if (enable) {
            _xExtent = 0;
            scrollTo(0, 0);
        }

        mFieldController.updateCaretRow();

        if (!makeCharVisible(mCaretPosition)) {
            invalidate();
        }
    }

    public float getZoom() {
        return mZoomFactor;
    }

    /**
     * Sets the text size to be factor of the base text size, scrolls the view
     * to display the caret if needed, and invalidates the entire view
     */
    public void setZoom(float factor) {
        if (factor <= 0.5 || factor >= 5 || factor == mZoomFactor) {
            return;
        }
        mZoomFactor = factor;
        int newSize = (int) (factor * BASE_TEXT_SIZE_PIXELS);
        mBrush.setTextSize(newSize);
        mBrushLine.setTextSize(newSize);
        if (mDoc.isWordWrap())
            mDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        mAlphaWidth = (int) mBrush.measureText("a");
        //if(!makeCharVisible(mCaretPosition)){
        invalidate();
        //}
    }

    /**
     * Sets the length of a tab character, scrolls the view to display the
     * caret if needed, and invalidates the entire view
     *
     * @param spaceCount The number of spaces a tab represents
     */
    public void setTabSpaces(int spaceCount) {
        if (spaceCount < 0) {
            return;
        }

        mTabLength = spaceCount;
        if (mDoc.isWordWrap())
            mDoc.analyzeWordWrap();
        mFieldController.updateCaretRow();
        if (!makeCharVisible(mCaretPosition)) {
            invalidate();
        }
    }

    /**
     * Enable/disable auto-indent
     */
    public void setAutoIndent(boolean enable) {
        isAutoIndent = enable;
    }

    /**
     * Enable/disable long-pressing capitalization.
     * When enabled, a long-press on a hardware key capitalizes that letter.
     * When disabled, a long-press on a hardware key bring up the
     * CharacterPickerDialog, if there are alternative characters to choose from.
     */
    public void setLongPressCaps(boolean enable) {
        isLongPressCaps = enable;
    }

    /**
     * Enable/disable highlighting of the current row. The current row is also
     * invalidated
     */
    public void setHighlightCurrentRow(boolean enable) {
        isHighlightRow = enable;
        invalidateCaretRow();
    }

    /**
     * Enable/disable display of visible representations of non-printing
     * characters like spaces, tabs and end of lines
     * Invalidates the view if the enable state changes
     */
    public void setNonPrintingCharVisibility(boolean enable) {
        if (enable ^ mShowNonPrinting) {
            mShowNonPrinting = enable;
            if (mDoc.isWordWrap())
                mDoc.analyzeWordWrap();
            mFieldController.updateCaretRow();
            if (!makeCharVisible(mCaretPosition)) {
                invalidate();
            }
        }
    }

    //---------------------------------------------------------------------
    //------------------------- Event handlers ----------------------------
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        //Intercept multiple key presses of printing characters to implement
        //long-press caps, because the IME may consume them and not pass the
        //event to onKeyDown() for long-press caps logic to work.
        //Technically, long-press caps should be implemented in the IME,
        //but is put here for end-user's convenience. Unfortunately this may
        //cause some IMEs to break. Remove this feature in future.
        if (isLongPressCaps
                && event.getRepeatCount() == 1
                && event.getAction() == KeyEvent.ACTION_DOWN) {

            char c = KeysInterpreter.keyEventToPrintableChar(event);
            if (Character.isLowerCase(c)
                    && c == Character.toLowerCase(mDoc.charAt(mCaretPosition - 1))) {
                mFieldController.onPrintableChar(BaseLanguage.BACKSPACE);
                mFieldController.onPrintableChar(Character.toUpperCase(c));
                return true;
            }
        }

        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Let touch navigation method intercept key event first
        if (mNaviMethod.onKeyDown(keyCode, event)) {
            return true;
        }

        //check if direction or symbol key
        if (KeysInterpreter.isNavigationKey(event)) {
            handleNavigationKey(keyCode, event);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SYM ||
                keyCode == KeyCharacterMap.PICKER_DIALOG_INPUT) {
            showCharacterPicker(
                    PICKER_SETS.get(KeyCharacterMap.PICKER_DIALOG_INPUT), false);
            return true;
        }

        //check if character is printable
        char c = KeysInterpreter.keyEventToPrintableChar(event);
        if (c == BaseLanguage.NULL_CHAR) {
            return super.onKeyDown(keyCode, event);
        }

        int repeatCount = event.getRepeatCount();
        //handle multiple (held) key presses
        if (repeatCount == 1) {
            if (isLongPressCaps) {
                handleLongPressCaps(c);
            } else {
                handleLongPressDialogDisplay(c);
            }
        } else if (repeatCount == 0
                || isLongPressCaps && !Character.isLowerCase(c)
                || !isLongPressCaps && PICKER_SETS.get(c) == null) {
            mFieldController.onPrintableChar(c);
        }

        return true;
    }

    private void handleNavigationKey(int keyCode, KeyEvent event) {
        if (event.isShiftPressed() && !isSelectText()) {
            invalidateCaretRow();
            mFieldController.setSelectText(true);
        } else if (!event.isShiftPressed() && isSelectText()) {
            invalidateSelectionRows();
            mFieldController.setSelectText(false);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mFieldController.moveCaretRight(false);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mFieldController.moveCaretLeft(false);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mFieldController.moveCaretDown();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                mFieldController.moveCaretUp();
                break;
            case KeyEvent.KEYCODE_ENTER:
                if (mAutoCompletePanel.isShow())
                    mAutoCompletePanel.selectFirst();
                break;
            default:
                break;
        }
    }

    private void handleLongPressCaps(char c) {
        if (Character.isLowerCase(c)
                && c == mDoc.charAt(mCaretPosition - 1)) {
            mFieldController.onPrintableChar(BaseLanguage.BACKSPACE);
            mFieldController.onPrintableChar(Character.toUpperCase(c));
        } else {
            mFieldController.onPrintableChar(c);
        }
    }

    //Precondition: If c is alphabetical, the character before the caret is
    //also c, which can be lower- or upper-case
    private void handleLongPressDialogDisplay(char c) {
        //workaround to get the appropriate caps mode to use
        boolean isCaps = Character.isUpperCase(mDoc.charAt(mCaretPosition - 1));
        char base = (isCaps) ? Character.toUpperCase(c) : c;

        String candidates = PICKER_SETS.get(base);
        if (candidates != null) {
            mFieldController.stopTextComposing();
            showCharacterPicker(candidates, true);
        } else {
            mFieldController.onPrintableChar(c);
        }
    }

    /**
     * @param candidates A string of characters to for the user to choose from
     * @param replace    If true, the character before the caret will be replaced
     *                   with the user-selected char. If false, the user-selected char will
     *                   be inserted at the caret position.
     */
    private void showCharacterPicker(String candidates, boolean replace) {
        final boolean shouldReplace = replace;
        final SpannableStringBuilder dummyString = new SpannableStringBuilder();
        Selection.setSelection(dummyString, 0);

        CharacterPickerDialog dialog = new CharacterPickerDialog(getContext(),
                this, dummyString, candidates, true);

        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (dummyString.length() > 0) {
                    if (shouldReplace) {
                        mFieldController.onPrintableChar(BaseLanguage.BACKSPACE);
                    }
                    mFieldController.onPrintableChar(dummyString.charAt(0));
                }
            }
        });
        dialog.show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (mNaviMethod.onKeyUp(keyCode, event)) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        int deltaX = Math.round(event.getX());
        int deltaY = Math.round(event.getY());
        while (deltaX > 0) {
            mFieldController.moveCaretRight(false);
            --deltaX;
        }
        while (deltaX < 0) {
            mFieldController.moveCaretLeft(false);
            ++deltaX;
        }
        while (deltaY > 0) {
            mFieldController.moveCaretDown();
            --deltaY;
        }
        while (deltaY < 0) {
            mFieldController.moveCaretUp();
            ++deltaY;
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isFocused()) {
            mNaviMethod.onTouchEvent(event);
        } else {
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP
                    && isPointInView((int) event.getX(), (int) event.getY())) {
                // somehow, the framework does not automatically change the focus
                // to this view when it is touched
                requestFocus();
            }
        }
        return true;
    }

    final private boolean isPointInView(int x, int y) {
        return (x >= 0 && x < getWidth() &&
                y >= 0 && y < getHeight());
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        invalidateCaretRow();
    }

    /**
     * Not public to allow access by {@link TouchNavigationMethod}
     */
    void showIME(boolean show) {
        InputMethodManager im = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (show) {
            im.showSoftInput(this, 0);
        } else {
            im.hideSoftInputFromWindow(this.getWindowToken(), 0);
        }
    }

    /**
     * Some navigation methods use sensors or have states for their widgets.
     * They should be notified of application lifecycle events so they can
     * start/stop sensing and load/store their GUI state.
     */
    void onPause() {
        mNaviMethod.onPause();
    }

    void onResume() {
        mNaviMethod.onResume();
    }

    void onDestroy() {
        mFieldController.cancelSpanning();
    }

    public Parcelable getUiState() {
        return new TextFieldUiState(this);
    }

    public void restoreUiState(Parcelable state) {
        TextFieldUiState uiState = (TextFieldUiState) state;
        final int caretPosition = uiState.mCaretPosition;
        // If the text field is in the process of being created, it may not
        // have its width and height set yet.
        // Therefore, post UI restoration tasks to run later.
        if (uiState._selectMode) {
            final int selStart = uiState._selectBegin;
            final int selEnd = uiState._selectEnd;

            post(new Runnable() {
                @Override
                public void run() {
                    setSelectionRange(selStart, selEnd - selStart);
                    if (caretPosition < selEnd) {
                        focusSelectionStart(); //caret at the end by default
                    }
                }
            });
        } else {
            post(new Runnable() {
                @Override
                public void run() {
                    moveCaret(caretPosition);
                }
            });
        }
    }

    //*********************************************************************
    //************************ Controller logic ***************************
    //*********************************************************************

    public synchronized BaseLexer getLexer() {
        return mFieldController.mLexer;
    }

    //切换Lexer
    public synchronized void setLexer(BaseLexer lexer) {
        if (lexer == null) {
            return;
        } else {
            final TextFieldController controller = mFieldController;
            if (controller.mLexer != null) {
                controller.mLexer.setCallback(null);
                controller.mLexer.onDisabled();
            }
            lexer.setCallback(controller);
            lexer.onEnabled();
            controller.mLexer = lexer;
            controller.determineSpans();
        }
    }

    public interface OnSizeChangedListener {
        void onSizeChanged(int w, int h, int oldw, int oldh);
    }

    //*********************************************************************
    //**************** UI State for saving and restoring ******************
    //*********************************************************************
    public static class TextFieldUiState implements Parcelable {
        public static final Creator<TextFieldUiState> CREATOR = new Creator<TextFieldUiState>() {
            @Override
            public TextFieldUiState createFromParcel(Parcel in) {
                return new TextFieldUiState(in);
            }

            @Override
            public TextFieldUiState[] newArray(int size) {
                return new TextFieldUiState[size];
            }
        };
        final int mCaretPosition;
        final int _scrollX;
        final int _scrollY;
        final boolean _selectMode;
        final int _selectBegin;
        final int _selectEnd;

        public TextFieldUiState(FreeScrollingTextField textField) {
            mCaretPosition = textField.getCaretPosition();
            _scrollX = textField.getScrollX();
            _scrollY = textField.getScrollY();
            _selectMode = textField.isSelectText();
            _selectBegin = textField.getSelectionStart();
            _selectEnd = textField.getSelectionEnd();
        }

        private TextFieldUiState(Parcel in) {
            mCaretPosition = in.readInt();
            _scrollX = in.readInt();
            _scrollY = in.readInt();
            _selectMode = in.readInt() != 0;
            _selectBegin = in.readInt();
            _selectEnd = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(mCaretPosition);
            out.writeInt(_scrollX);
            out.writeInt(_scrollY);
            out.writeInt(_selectMode ? 1 : 0);
            out.writeInt(_selectBegin);
            out.writeInt(_selectEnd);
        }

    }

    private class TextFieldController
            implements BaseLexer.LexCallback {
        private boolean isInSelectionMode = false;
        private BaseLexer mLexer = BaseLexer.newInstance(this);
        private boolean isInSelectionMode2;

        /**
         * Analyze the text for programming language keywords and redraws the
         * text view when done. The global programming language used is set with
         * the static method Lexer.setLanguage(Language)
         * <p>
         * Does nothing if the Lexer language is not a programming language
         */
        public void determineSpans() {
            mLexer.tokenize(mDoc);
        }

        public void cancelSpanning() {
            mLexer.cancelTokenize();
        }

        @Override
        //This is usually called from a non-UI thread
        public void lexDone(final List<Pair> results) {
            post(new Runnable() {
                @Override
                public void run() {
                    mDoc.setSpans(results);
                    invalidate();
                }
            });
        }

        //- TextFieldController -----------------------------------------------
        //---------------------------- Key presses ----------------------------

        public void onPrintableChar(char c) {

            // delete currently selected text, if any
            boolean selectionDeleted = false;
            if (isInSelectionMode) {
                selectionDelete();
                selectionDeleted = true;
            }

            int originalRow = mCaretRow;
            int originalOffset = mDoc.getRowOffset(originalRow);

            switch (c) {
                case BaseLanguage.BACKSPACE:
                    if (selectionDeleted) {
                        break;
                    }

                    if (mCaretPosition > 0) {
                        mDoc.deleteAt(mCaretPosition - 1, System.nanoTime());
                        if (mDoc.charAt(mCaretPosition - 2) == 0xd83d || mDoc.charAt(mCaretPosition - 2) == 0xd83c) {
                            mDoc.deleteAt(mCaretPosition - 2, System.nanoTime());
                            moveCaretLeft(true);
                        }

                        mTextListener.onDel(c + "", mCaretPosition, 1);
                        moveCaretLeft(true);

                        if (mCaretRow < originalRow) {
                            // either a newline was deleted or the caret was on the
                            // first word and it became short enough to fit the prev
                            // row
                            invalidateFromRow(mCaretRow);
                        } else if (mDoc.isWordWrap()) {
                            if (originalOffset != mDoc.getRowOffset(originalRow)) {
                                //invalidate previous row too if its wrapping changed
                                --originalRow;
                            }
                            invalidateFromRow(originalRow);
                        }
                    }
                    break;

                case BaseLanguage.NEWLINE:
                    if (isAutoIndent) {
                        char[] indent = createAutoIndent();
                        mDoc.insertBefore(indent, mCaretPosition, System.nanoTime());
                        moveCaret(mCaretPosition + indent.length);
                    } else {
                        mDoc.insertBefore(c, mCaretPosition, System.nanoTime());
                        moveCaretRight(true);
                    }

                    if (mDoc.isWordWrap() && originalOffset != mDoc.getRowOffset(originalRow)) {
                        //invalidate previous row too if its wrapping changed
                        --originalRow;
                    }

                    mTextListener.onNewLine(c + "", mCaretPosition, 1);

                    invalidateFromRow(originalRow);
                    break;

                default:
                    mDoc.insertBefore(c, mCaretPosition, System.nanoTime());
                    moveCaretRight(true);
                    mTextListener.onAdd(c + "", mCaretPosition, 1);

                    if (mDoc.isWordWrap()) {
                        if (originalOffset != mDoc.getRowOffset(originalRow)) {
                            //invalidate previous row too if its wrapping changed
                            --originalRow;
                        }
                        invalidateFromRow(originalRow);
                    }
                    break;
            }

            setEdited(true);
            determineSpans();
        }

        /**
         * Return a char[] with a newline as the 0th element followed by the
         * leading spaces and tabs of the line that the caret is on
         * 创建自动缩进
         */
        private char[] createAutoIndent() {
            int lineNum = mDoc.findLineNumber(mCaretPosition);
            int startOfLine = mDoc.getLineOffset(lineNum);
            int whitespaceCount = 0;
            mDoc.seekChar(startOfLine);
            while (mDoc.hasNext()) {
                char c = mDoc.next();
                if ((c != ' ' && c != BaseLanguage.TAB) || startOfLine + whitespaceCount >= mCaretPosition) {
                    break;
                }
                ++whitespaceCount;
            }

            whitespaceCount += getLexer().autoIndent(mDoc.subSequence(startOfLine, mCaretPosition - startOfLine));
            if (whitespaceCount < 0)
                return new char[]{BaseLanguage.NEWLINE};

            char[] indent = new char[1 + whitespaceCount];
            indent[0] = BaseLanguage.NEWLINE;

            mDoc.seekChar(startOfLine);
            for (int i = 0; i < whitespaceCount; ++i) {
                indent[1 + i] = ' ';
            }
            return indent;
        }

        public void moveCaretDown() {
            if (!caretOnLastRowOfFile()) {
                int currCaret = mCaretPosition;
                int currRow = mCaretRow;
                int newRow = currRow + 1;
                int currColumn = getColumn(currCaret);
                int currRowLength = mDoc.getRowSize(currRow);
                int newRowLength = mDoc.getRowSize(newRow);

                if (currColumn < newRowLength) {
                    // Position at the same column as old row.
                    mCaretPosition += currRowLength;
                } else {
                    // Column does not exist in the new row (new row is too short).
                    // Position at end of new row instead.
                    mCaretPosition +=
                            currRowLength - currColumn + newRowLength - 1;
                }
                ++mCaretRow;

                updateSelectionRange(currCaret, mCaretPosition);
                if (!makeCharVisible(mCaretPosition)) {
                    invalidateRows(currRow, newRow + 1);
                }
                mRowListener.onRowChange(newRow);
                stopTextComposing();
            }
        }

        public void moveCaretUp() {
            if (!caretOnFirstRowOfFile()) {
                int currCaret = mCaretPosition;
                int currRow = mCaretRow;
                int newRow = currRow - 1;
                int currColumn = getColumn(currCaret);
                int newRowLength = mDoc.getRowSize(newRow);

                if (currColumn < newRowLength) {
                    // Position at the same column as old row.
                    mCaretPosition -= newRowLength;
                } else {
                    // Column does not exist in the new row (new row is too short).
                    // Position at end of new row instead.
                    mCaretPosition -= (currColumn + 1);
                }
                --mCaretRow;

                updateSelectionRange(currCaret, mCaretPosition);
                if (!makeCharVisible(mCaretPosition)) {
                    invalidateRows(newRow, currRow + 1);
                }
                mRowListener.onRowChange(newRow);
                stopTextComposing();
            }
        }

        /**
         * @param isTyping Whether caret is moved to a consecutive position as
         *                 a result of entering text
         */
        public void moveCaretRight(boolean isTyping) {
            if (!caretOnEOF()) {
                int originalRow = mCaretRow;
                ++mCaretPosition;
                updateCaretRow();
                updateSelectionRange(mCaretPosition - 1, mCaretPosition);
                if (!makeCharVisible(mCaretPosition)) {
                    invalidateRows(originalRow, mCaretRow + 1);
                }

                if (!isTyping) {
                    stopTextComposing();
                }
            }
        }

        /**
         * @param isTyping Whether caret is moved to a consecutive position as
         *                 a result of deleting text
         */
        public void moveCaretLeft(boolean isTyping) {
            if (mCaretPosition > 0) {
                int originalRow = mCaretRow;
                --mCaretPosition;
                updateCaretRow();
                updateSelectionRange(mCaretPosition + 1, mCaretPosition);
                if (!makeCharVisible(mCaretPosition)) {
                    invalidateRows(mCaretRow, originalRow + 1);
                }

                if (!isTyping) {
                    stopTextComposing();
                }
            }
        }

        public void moveCaret(int i) {
            if (i < 0 || i >= mDoc.docLength()) {
                TextWarriorException.fail("Invalid caret position");
                return;
            }

            updateSelectionRange(mCaretPosition, i);
            mCaretPosition = i;
            updateAfterCaretJump();
        }

        private void updateAfterCaretJump() {
            int oldRow = mCaretRow;
            updateCaretRow();
            if (!makeCharVisible(mCaretPosition)) {
                invalidateRows(oldRow, oldRow + 1); //old caret row
                invalidateCaretRow(); //new caret row
            }
            stopTextComposing();
        }


        /**
         * This helper method should only be used by internal methods after ic_setting
         * mCaretPosition, in order to to recalculate the new row the caret is on.
         */
        void updateCaretRow() {
            int newRow = mDoc.findRowNumber(mCaretPosition);
            if (mCaretRow != newRow) {
                mCaretRow = newRow;
                mRowListener.onRowChange(newRow);
            }
        }

        public void stopTextComposing() {
            InputMethodManager im = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            // This is an overkill way to inform the InputMethod that the caret
            // might have changed position and it should re-evaluate the
            // caps mode to use.
            im.restartInput(FreeScrollingTextField.this);

            if (mInputConnection != null && mInputConnection.isComposingStarted()) {
                mInputConnection.resetComposingState();
            }
        }

        //- TextFieldController -----------------------------------------------
        //-------------------------- Selection mode ---------------------------
        public final boolean isSelectText() {
            return isInSelectionMode;
        }

        /**
         * Enter or exit select mode.
         * Does not invalidate view.
         *
         * @param mode If true, enter select mode; else exit select mode
         */
        public void setSelectText(boolean mode) {
            if (!(mode ^ isInSelectionMode)) {
                return;
            }

            if (mode) {
                mSelectionAnchor = mCaretPosition;
                mSelectionEdge = mCaretPosition;
            } else {
                mSelectionAnchor = -1;
                mSelectionEdge = -1;
            }
            isInSelectionMode = mode;
            isInSelectionMode2 = mode;
            mSelModeLis.onSelectionChanged(mode, getSelectionStart(), getSelectionEnd());
        }

        public final boolean isSelectText2() {
            return isInSelectionMode2;
        }

        public boolean inSelectionRange(int charOffset) {
            if (mSelectionAnchor < 0) {
                return false;
            }

            return (mSelectionAnchor <= charOffset &&
                    charOffset < mSelectionEdge);
        }

        /**
         * Selects numChars count of characters starting from beginPosition.
         * Invalidates necessary areas.
         *
         * @param beginPosition
         * @param numChars
         * @param scrollToStart If true, the start of the selection will be scrolled
         *                      into view. Otherwise, the end of the selection will be scrolled.
         */
        public void setSelectionRange(int beginPosition, int numChars,
                                      boolean scrollToStart, boolean mode) {
            TextWarriorException.assertVerbose(
                    (beginPosition >= 0) && numChars <= (mDoc.docLength() - 1) && numChars >= 0,
                    "Invalid range to select");

            if (isInSelectionMode) {
                // unhighlight previous selection
                invalidateSelectionRows();
            } else {
                // unhighlight caret
                invalidateCaretRow();
                if (mode)
                    setSelectText(true);
                else
                    isInSelectionMode = true;
            }

            mSelectionAnchor = beginPosition;
            mSelectionEdge = mSelectionAnchor + numChars;

            mCaretPosition = mSelectionEdge;
            stopTextComposing();
            updateCaretRow();
            if (mode)
                mSelModeLis.onSelectionChanged(isSelectText(), mSelectionAnchor, mSelectionEdge);
            boolean scrolled = makeCharVisible(mSelectionEdge);

            if (scrollToStart) {
                // the beginning of multi-line selections as far left as possible
                scrolled = makeCharVisible(mSelectionAnchor);
            }

            if (!scrolled) {
                invalidateSelectionRows();
            }
        }

        /**
         * Moves the caret to an edge of selected text and scrolls it to view.
         *
         * @param start If true, moves the caret to the beginning of
         *              the selection. Otherwise, moves the caret to the end of the selection.
         *              In all cases, the caret is scrolled to view if it is not visible.
         */
        public void focusSelection(boolean start) {
            if (isInSelectionMode) {
                if (start && mCaretPosition != mSelectionAnchor) {
                    mCaretPosition = mSelectionAnchor;
                    updateAfterCaretJump();
                } else if (!start && mCaretPosition != mSelectionEdge) {
                    mCaretPosition = mSelectionEdge;
                    updateAfterCaretJump();
                }
            }
        }


        /**
         * Used by internal methods to update selection boundaries when a new
         * caret position is set.
         * Does nothing if not in selection mode.
         */
        private void updateSelectionRange(int oldCaretPosition, int newCaretPosition) {
            if (!isInSelectionMode) {
                return;
            }

            if (oldCaretPosition < mSelectionEdge) {
                if (newCaretPosition > mSelectionEdge) {
                    mSelectionAnchor = mSelectionEdge;
                    mSelectionEdge = newCaretPosition;
                } else {
                    mSelectionAnchor = newCaretPosition;
                }

            } else {
                if (newCaretPosition < mSelectionAnchor) {
                    mSelectionEdge = mSelectionAnchor;
                    mSelectionAnchor = newCaretPosition;
                } else {
                    mSelectionEdge = newCaretPosition;
                }
            }
        }


        //- TextFieldController -----------------------------------------------
        //------------------------ Cut, copy, paste ---------------------------

        /**
         * Convenience method for consecutive copy and paste calls
         */
        public void cut(ClipboardManager cb) {
            copy(cb);
            selectionDelete();
        }

        /**
         * Copies the selected text to the clipboard.
         * <p>
         * Does nothing if not in select mode.
         */
        public void copy(ClipboardManager cb) {
            if (isInSelectionMode &&
                    mSelectionAnchor < mSelectionEdge) {
                CharSequence contents = mDoc.subSequence(mSelectionAnchor,
                        mSelectionEdge - mSelectionAnchor);
                cb.setText(contents);
            }
        }

        /**
         * Inserts text at the caret position.
         * Existing selected text will be deleted and select mode will end.
         * The deleted area will be invalidated.
         * <p>
         * After insertion, the inserted area will be invalidated.
         */
        public void paste(String text) {
            if (text == null) {
                return;
            }

            mDoc.beginBatchEdit();
            selectionDelete();

            int originalRow = mCaretRow;
            int originalOffset = mDoc.getRowOffset(originalRow);
            mDoc.insertBefore(text.toCharArray(), mCaretPosition, System.nanoTime());
            mTextListener.onAdd(text, mCaretPosition, text.length());
            mDoc.endBatchEdit();

            mCaretPosition += text.length();
            updateCaretRow();

            setEdited(true);
            determineSpans();
            stopTextComposing();

            if (!makeCharVisible(mCaretPosition)) {
                int invalidateStartRow = originalRow;
                //invalidate previous row too if its wrapping changed
                if (mDoc.isWordWrap() &&
                        originalOffset != mDoc.getRowOffset(originalRow)) {
                    --invalidateStartRow;
                }

                if (originalRow == mCaretRow && !mDoc.isWordWrap()) {
                    //pasted text only affects caret row
                    invalidateRows(invalidateStartRow, invalidateStartRow + 1);
                } else {
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        /**
         * Deletes selected text, exits select mode and invalidates deleted area.
         * If the selected range is empty, this method exits select mode and
         * invalidates the caret.
         * <p>
         * Does nothing if not in select mode.
         */
        public void selectionDelete() {
            if (!isInSelectionMode) {
                return;
            }

            int totalChars = mSelectionEdge - mSelectionAnchor;

            if (totalChars > 0) {
                int originalRow = mDoc.findRowNumber(mSelectionAnchor);
                int originalOffset = mDoc.getRowOffset(originalRow);
                boolean isSingleRowSel = mDoc.findRowNumber(mSelectionEdge) == originalRow;
                mDoc.deleteAt(mSelectionAnchor, totalChars, System.nanoTime());
                mTextListener.onDel("", mCaretPosition, totalChars);
                mCaretPosition = mSelectionAnchor;
                updateCaretRow();
                setEdited(true);
                determineSpans();
                setSelectText(false);
                stopTextComposing();

                if (!makeCharVisible(mCaretPosition)) {
                    int invalidateStartRow = originalRow;
                    //invalidate previous row too if its wrapping changed
                    if (mDoc.isWordWrap() &&
                            originalOffset != mDoc.getRowOffset(originalRow)) {
                        --invalidateStartRow;
                    }

                    if (isSingleRowSel && !mDoc.isWordWrap()) {
                        //pasted text only affects current row
                        invalidateRows(invalidateStartRow, invalidateStartRow + 1);
                    } else {
                        invalidateFromRow(invalidateStartRow);
                    }
                }
            } else {
                setSelectText(false);
                invalidateCaretRow();
            }
        }

        void replaceText(int from, int charCount, String text) {
            int invalidateStartRow, originalOffset;
            boolean isInvalidateSingleRow = true;
            boolean dirty = false;

            //delete selection
            if (isInSelectionMode) {
                invalidateStartRow = mDoc.findRowNumber(mSelectionAnchor);
                originalOffset = mDoc.getRowOffset(invalidateStartRow);

                int totalChars = mSelectionEdge - mSelectionAnchor;

                if (totalChars > 0) {
                    mCaretPosition = mSelectionAnchor;
                    mDoc.deleteAt(mSelectionAnchor, totalChars, System.nanoTime());

                    if (invalidateStartRow != mCaretRow) {
                        isInvalidateSingleRow = false;
                    }
                    dirty = true;
                }

                setSelectText(false);
            } else {
                invalidateStartRow = mCaretRow;
                originalOffset = mDoc.getRowOffset(mCaretRow);
            }

            //delete requested chars
            if (charCount > 0) {
                int delFromRow = mDoc.findRowNumber(from);
                if (delFromRow < invalidateStartRow) {
                    invalidateStartRow = delFromRow;
                    originalOffset = mDoc.getRowOffset(delFromRow);
                }

                if (invalidateStartRow != mCaretRow) {
                    isInvalidateSingleRow = false;
                }

                mCaretPosition = from;
                mDoc.deleteAt(from, charCount, System.nanoTime());
                dirty = true;
            }

            //insert
            if (text != null && text.length() > 0) {
                int insFromRow = mDoc.findRowNumber(from);
                if (insFromRow < invalidateStartRow) {
                    invalidateStartRow = insFromRow;
                    originalOffset = mDoc.getRowOffset(insFromRow);
                }

                mDoc.insertBefore(text.toCharArray(), mCaretPosition, System.nanoTime());
                mCaretPosition += text.length();
                dirty = true;
            }

            if (dirty) {
                setEdited(true);
                determineSpans();
            }

            int originalRow = mCaretRow;
            updateCaretRow();
            if (originalRow != mCaretRow) {
                isInvalidateSingleRow = false;
            }

            if (!makeCharVisible(mCaretPosition)) {
                //invalidate previous row too if its wrapping changed
                if (mDoc.isWordWrap() &&
                        originalOffset != mDoc.getRowOffset(invalidateStartRow)) {
                    --invalidateStartRow;
                }

                if (isInvalidateSingleRow && !mDoc.isWordWrap()) {
                    //replaced text only affects current row
                    invalidateRows(mCaretRow, mCaretRow + 1);
                } else {
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        //- TextFieldController -----------------------------------------------
        //----------------- Helper methods for InputConnection ----------------

        /**
         * Deletes existing selected text, then deletes charCount number of
         * characters starting at from, and inserts text in its place.
         * <p>
         * Unlike paste or selectionDelete, does not signal the end of
         * text composing to the IME.
         */
        void replaceComposingText(int from, int charCount, String text) {
            int invalidateStartRow, originalOffset;
            boolean isInvalidateSingleRow = true;
            boolean dirty = false;

            //delete selection
            if (isInSelectionMode) {
                invalidateStartRow = mDoc.findRowNumber(mSelectionAnchor);
                originalOffset = mDoc.getRowOffset(invalidateStartRow);

                int totalChars = mSelectionEdge - mSelectionAnchor;

                if (totalChars > 0) {
                    mCaretPosition = mSelectionAnchor;
                    mDoc.deleteAt(mSelectionAnchor, totalChars, System.nanoTime());

                    if (invalidateStartRow != mCaretRow) {
                        isInvalidateSingleRow = false;
                    }
                    dirty = true;
                }

                setSelectText(false);
            } else {
                invalidateStartRow = mCaretRow;
                originalOffset = mDoc.getRowOffset(mCaretRow);
            }

            //delete requested chars
            if (charCount > 0) {
                int delFromRow = mDoc.findRowNumber(from);
                if (delFromRow < invalidateStartRow) {
                    invalidateStartRow = delFromRow;
                    originalOffset = mDoc.getRowOffset(delFromRow);
                }

                if (invalidateStartRow != mCaretRow) {
                    isInvalidateSingleRow = false;
                }

                mCaretPosition = from;
                mDoc.deleteAt(from, charCount, System.nanoTime());
                dirty = true;
            }

            //insert
            if (text != null && text.length() > 0) {
                int insFromRow = mDoc.findRowNumber(from);
                if (insFromRow < invalidateStartRow) {
                    invalidateStartRow = insFromRow;
                    originalOffset = mDoc.getRowOffset(insFromRow);
                }

                mDoc.insertBefore(text.toCharArray(), mCaretPosition, System.nanoTime());
                mCaretPosition += text.length();
                dirty = true;
            }

            mTextListener.onAdd(text, mCaretPosition, text.length() - charCount);
            if (dirty) {
                setEdited(true);
                determineSpans();
            }

            int originalRow = mCaretRow;
            updateCaretRow();
            if (originalRow != mCaretRow) {
                isInvalidateSingleRow = false;
            }

            if (!makeCharVisible(mCaretPosition)) {
                //invalidate previous row too if its wrapping changed
                if (mDoc.isWordWrap() &&
                        originalOffset != mDoc.getRowOffset(invalidateStartRow)) {
                    --invalidateStartRow;
                }

                if (isInvalidateSingleRow && !mDoc.isWordWrap()) {
                    //replaced text only affects current row
                    invalidateRows(mCaretRow, mCaretRow + 1);
                } else {
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        /**
         * Delete leftLength characters of text before the current caret
         * position, and delete rightLength characters of text after the current
         * cursor position.
         * <p>
         * Unlike paste or selectionDelete, does not signal the end of
         * text composing to the IME.
         */
        void deleteAroundComposingText(int left, int right) {
            int start = mCaretPosition - left;
            if (start < 0) {
                start = 0;
            }
            int end = mCaretPosition + right;
            int docLength = mDoc.docLength();
            if (end > (docLength - 1)) { //exclude the terminal EOF
                end = docLength - 1;
            }
            replaceComposingText(start, end - start, "");
        }

        String getTextAfterCursor(int maxLen) {
            int docLength = mDoc.docLength();
            if ((mCaretPosition + maxLen) > (docLength - 1)) {
                //exclude the terminal EOF
                return mDoc.subSequence(mCaretPosition, docLength - mCaretPosition - 1).toString();
            }

            return mDoc.subSequence(mCaretPosition, maxLen).toString();
        }

        String getTextBeforeCursor(int maxLen) {
            int start = mCaretPosition - maxLen;
            if (start < 0) {
                start = 0;
            }
            return mDoc.subSequence(start, mCaretPosition - start).toString();
        }
    }//end inner controller class

    //*********************************************************************
    //************************** InputConnection **************************
    //*********************************************************************
    /*
     * Does not provide ExtractedText related methods
     */
    private class TextFieldInputConnection extends BaseInputConnection {
        private boolean _isComposing = false;
        private int _composingCharCount = 0;

        public TextFieldInputConnection(FreeScrollingTextField v) {
            super(v, true);
        }

        public void resetComposingState() {
            _composingCharCount = 0;
            _isComposing = false;
            mDoc.endBatchEdit();
        }

        @Override
        public boolean performContextMenuAction(int id) {
            switch (id) {
                case android.R.id.copy:
                    copy();
                    break;
                case android.R.id.cut:
                    cut();
                    break;
                case android.R.id.paste:
                    paste();
                    break;
                case android.R.id.startSelectingText:
                case android.R.id.stopSelectingText:
                case android.R.id.selectAll:
                    selectAll();
                    break;
            }

            return false;
        }

        public boolean sendKeyEvent(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                    if (isSelectText())
                        selectText(false);
                    else
                        selectText(true);
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    moveCaretLeft();
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    moveCaretUp();
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    moveCaretRight();
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    moveCaretDown();
                    break;
                case KeyEvent.KEYCODE_MOVE_HOME:
                    moveCaret(0);
                    break;
                case KeyEvent.KEYCODE_MOVE_END:
                    moveCaret(mDoc.length() - 1);
                    break;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (mAutoCompletePanel.isShow()) {
                        mAutoCompletePanel.selectFirst();
                    } else {
                        return super.sendKeyEvent(event);
                    }
                    break;
                case KeyEvent.KEYCODE_DEL:
                    int count = getSpace();
                    if (count != -1) {
                        for (int i = 0; i < count - 1; i++) {
                            super.sendKeyEvent(event);
                        }
                        return super.sendKeyEvent(event);
                    } else {
                        return super.sendKeyEvent(event);
                    }
                default:
                    return super.sendKeyEvent(event);
            }
            return true;
        }

        private int getSpace() {
            int count = 0;
            JavaLexer lexer = new JavaLexer(mDoc.getLine(getCaretRow()));
            JavaType type = null;
            while (true) {
                try {
                    if (!((type = lexer.yylex()) != JavaType.EOF)) break;
                    switch (type) {
                        case WHITESPACE:
                        case NEWLINE:
                            count++;
                            break;
                        default:
                            return -1;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return -1;
                }
            }
            return count;
        }

        /**
         * Only true when the InputConnection has not been used by the IME yet.
         * Can be programatically cleared by resetComposingState()
         */
        public boolean isComposingStarted() {
            return _isComposing;
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            _isComposing = true;
            if (!mDoc.isBatchEdit()) {
                mDoc.beginBatchEdit();
            }

            mFieldController.replaceComposingText(
                    getCaretPosition() - _composingCharCount,
                    _composingCharCount,
                    text.toString());
            _composingCharCount = text.length();

            if (newCursorPosition > 1) {
                mFieldController.moveCaret(mCaretPosition + newCursorPosition - 1);
            } else if (newCursorPosition <= 0) {
                mFieldController.moveCaret(mCaretPosition - text.length() - newCursorPosition);
            }
            return true;
        }

        /**
         * 输入法传递过来的字符串
         *
         * @param text
         * @param newCursorPosition
         * @return
         */
        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            mFieldController.replaceComposingText(
                    getCaretPosition() - _composingCharCount,
                    _composingCharCount,
                    text.toString());
            _composingCharCount = 0;
            mDoc.endBatchEdit();
            if (newCursorPosition > 1) {
                mFieldController.moveCaret(mCaretPosition + newCursorPosition - 1);
            }
//			else if(newCursorPosition==1){
//				mFieldController.moveCaret(getCaretPosition() + newCursorPosition);
//			}
            else if (newCursorPosition <= 0) {
                mFieldController.moveCaret(mCaretPosition - text.length() - newCursorPosition);
            }
            _isComposing = false;

            return true;
        }


        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength) {
            mFieldController.deleteAroundComposingText(leftLength, rightLength);
            return true;
        }

        @Override
        public boolean finishComposingText() {
            resetComposingState();
            return true;
        }

        @Override
        public int getCursorCapsMode(int reqModes) {
            int capsMode = 0;

            // Ignore InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS; not used in TextWarrior

            if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                    == InputType.TYPE_TEXT_FLAG_CAP_WORDS) {
                int prevChar = mCaretPosition - 1;
                if (prevChar < 0 || getLanguage().isWhitespace(mDoc.charAt(prevChar))) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;

                    //set CAP_SENTENCES if client is interested in it
                    if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                            == InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) {
                        capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                    }
                }
            }

            // Strangely, Android soft keyboard does not set TYPE_TEXT_FLAG_CAP_SENTENCES
            // in reqModes even if it is interested in doing auto-capitalization.
            // Android bug? Therefore, we assume TYPE_TEXT_FLAG_CAP_SENTENCES
            // is always set to be on the safe side.
            else {
                BaseLanguage lang = getLanguage();

                int prevChar = mCaretPosition - 1;
                int whitespaceCount = 0;
                boolean capsOn = true;

                // Turn on caps mode only for the first char of a sentence.
                // A fresh line is also considered to start a new sentence.
                // The position immediately after a period is considered lower-case.
                // Examples: "abc.com" but "abc. Com"
                while (prevChar >= 0) {
                    char c = mDoc.charAt(prevChar);
                    if (c == BaseLanguage.NEWLINE) {
                        break;
                    }

                    if (!lang.isWhitespace(c)) {
                        if (whitespaceCount == 0 || !lang.isSentenceTerminator(c)) {
                            capsOn = false;
                        }
                        break;
                    }

                    ++whitespaceCount;
                    --prevChar;
                }

                if (capsOn) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                }
            }

            return capsMode;
        }

        @Override
        public CharSequence getTextAfterCursor(int maxLen, int flags) {
            return mFieldController.getTextAfterCursor(maxLen); //ignore flags
        }

        @Override
        public CharSequence getTextBeforeCursor(int maxLen, int flags) {
            return mFieldController.getTextBeforeCursor(maxLen); //ignore flags
        }

        @Override
        public boolean setSelection(int start, int end) {
            if (start == end) {
                if (start == 0) {
                    //适配搜狗输入法
                    if (getCaretPosition() > 0) {
                        mFieldController.moveCaret(getCaretPosition() - 1);
                    }
                } else {
                    mFieldController.moveCaret(start);
                }
            } else {
                mFieldController.setSelectionRange(start, end - start, false, true);
            }
            return true;
        }

    }// end inner class
}

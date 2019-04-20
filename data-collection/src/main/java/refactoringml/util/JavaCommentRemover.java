// from https://gist.githubusercontent.com/riversun/aece01e8b30841a1921f9926c0ea713d/raw/796c767b7b035ae0df814e219a2f39981c3474ea/JavaCommentRemover.java

/**
 * Copyright 2006-2016 Tom Misawa(riversun.org@gmail.com)
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package refactoringml.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaCommentRemover To remove comments from java source code<br>
 * To replace comments as you like<br>
 *
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class JavaCommentRemover {

	/**
	 * Specify NEWLINE according to your code environment
	 */
	public static String NEWLINE = "\r\n";

	/**
	 *
	 * Tag that is enclosed by some strings
	 *
	 */
	private static class TagType {
		CodeType codeType;
		String tagBegin;
		String tagEnd;
	}

	/**
	 * Code Type
	 */
	public enum CodeType {
		EXECUTABLE_CODE, // Normal Code.Not a comment
		COMMENT, // One line comment
		BLOCK_COMMENT, // BlockComemnt Code
		JAVADOC_COMMENT// JavaDoc Code
	}

	/**
	 * Callback interface When found the comment
	 */
	public static interface CommentListener {
		/**
		 * When comment found
		 *
		 * @param commentType
		 * @param comment
		 * @return returns comment to replace.If null,the comment will be
		 *         removed from the source code<br>
		 */
		public String onCommentFound(CodeType commentType, String comment);
	}

	private CodeType mScanMode = CodeType.EXECUTABLE_CODE;
	private String mCurrentSrcCode = "";
	private int mCurrentSrcCodeLen = 0;
	private CommentListener mCommentListener = null;
	private List<TagType> mTagTypeList = new ArrayList<TagType>();
	private Map<CodeType, StringBuilder> mBufferMap = new HashMap<CodeType, StringBuilder>();

	public JavaCommentRemover() {
		initialize();
	}

	public void setCommentListener(CommentListener commentListener) {
		mCommentListener = commentListener;
	}

	/**
	 * Get comment removed/comment replaced source code
	 *
	 * @param sourceCode
	 * @return edited sourceCode
	 */
	public String removeComment(String sourceCode) {

		mCurrentSrcCode = sourceCode;
		mCurrentSrcCodeLen = sourceCode.length();

		int index = 0;

		charScanLoop: while (index < mCurrentSrcCodeLen) {

			for (TagType tagType : mTagTypeList) {

				// if [tagBegin] found
				if (isStartsWith(index, tagType.tagBegin)) {

					if (CodeType.EXECUTABLE_CODE == mScanMode) {
						index += lenthOf(tagType.tagBegin);
						mScanMode = tagType.codeType;
						continue charScanLoop;
					}
				}
				// if [tagEnd] found
				else if (isStartsWith(index, tagType.tagEnd)) {

					if (tagType.codeType == mScanMode) {
						index += lenthOf(tagType.tagEnd);
						mScanMode = CodeType.EXECUTABLE_CODE;

						final String blockComment = tagType.tagBegin + getCommentBuffer(tagType.codeType).toString() + tagType.tagEnd;
						final String commentToReplace = onCommentFound(tagType.codeType, blockComment);

						if (commentToReplace != null) {
							getCommentBuffer(CodeType.EXECUTABLE_CODE).append(commentToReplace);
						}

						// clear buffer
						getCommentBuffer(tagType.codeType).setLength(0);

						continue charScanLoop;
					}
				}

			}

			final String currentChar = stringAt(index);

			getCommentBuffer(mScanMode).append(currentChar);

			index++;

		}

		return getCommentBuffer(CodeType.EXECUTABLE_CODE).toString();
	}

	/**
	 * Register comment tags<br>
	 * You can add the original tags
	 */
	private void initialize() {

		// Register JavaDoc Comment Tag
		final String JAVADOC_COMMENT_STARTED = "/**";
		final String JAVADOC_COMMENT_FINISHED = "*/";
		final TagType tagTypeJavaDocComment = new TagType();
		tagTypeJavaDocComment.codeType = CodeType.JAVADOC_COMMENT;
		tagTypeJavaDocComment.tagBegin = JAVADOC_COMMENT_STARTED;
		tagTypeJavaDocComment.tagEnd = JAVADOC_COMMENT_FINISHED;
		mTagTypeList.add(tagTypeJavaDocComment);

		// Register Block Comment Tag
		final String BLOCK_COMMENT_STARTED = "/*";
		final String BLOCK_COMMENT_FINISHED = "*/";
		final TagType tagTypeBlockComment = new TagType();
		tagTypeBlockComment.codeType = CodeType.BLOCK_COMMENT;
		tagTypeBlockComment.tagBegin = BLOCK_COMMENT_STARTED;
		tagTypeBlockComment.tagEnd = BLOCK_COMMENT_FINISHED;
		mTagTypeList.add(tagTypeBlockComment);

		// Register Comment Tag
		final String COMMENT_STARTED = "//";
		final String COMMENT_FINISHED = NEWLINE;
		final TagType tagTypeNormalComment = new TagType();
		tagTypeNormalComment.codeType = CodeType.COMMENT;
		tagTypeNormalComment.tagBegin = COMMENT_STARTED;
		tagTypeNormalComment.tagEnd = COMMENT_FINISHED;
		mTagTypeList.add(tagTypeNormalComment);
	}

	private String onCommentFound(CodeType commentType, String comment) {

		if (mCommentListener != null) {
			return mCommentListener.onCommentFound(commentType, comment);
		}
		return null;
	}

	private StringBuilder getCommentBuffer(CodeType mode) {

		StringBuilder buffer = mBufferMap.get(mode);

		if (buffer == null) {
			buffer = new StringBuilder();
			mBufferMap.put(mode, buffer);
		}
		return buffer;

	}

	/**
	 * Get string at specified position
	 *
	 * @param index
	 * @return
	 */
	private String stringAt(int index) {

		final char charAt = mCurrentSrcCode.charAt(index);
		return String.valueOf(charAt);

	}

	private boolean isStartsWith(int fromIndex, String text) {

		final int textLen = text.length();

		final StringBuilder target = new StringBuilder();

		final int fromPos = fromIndex;
		final int toPos = fromIndex + textLen;

		if (toPos >= mCurrentSrcCodeLen) {
			return false;
		}

		for (int i = fromPos; i < toPos; i++) {
			target.append(stringAt(i));
		}

		if (target.toString().equals(text)) {
			return true;
		} else {
			return false;
		}
	}

	private int lenthOf(String str) {
		return str.length();
	}

	// ///////////////////////////////////////////////////////////////
	// Portion of TextReader(https://gist.github.com/riversun)
	// ///////////////////////////////////////////////////////////////

	/**
	 * Read whole text from input stream char by char
	 *
	 * @param is
	 * @param charset
	 *            specify character set like 'UTF-8'
	 * @return
	 * @throws IOException
	 */
	public static String readText(InputStream is, String charset) throws IOException {

		final StringBuilder sb = new StringBuilder();

		InputStreamReader isr = null;
		BufferedReader br = null;

		try {

			if (charset != null) {
				isr = new InputStreamReader(is, charset);
			} else {
				isr = new InputStreamReader(is);
			}

			br = new BufferedReader(isr);

			int iChar = br.read();

			while (iChar != -1) {
				sb.append((char) iChar);
				iChar = br.read();
			}

		} finally {

			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
			if (isr != null) {
				try {
					isr.close();
				} catch (IOException e) {
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		return sb.toString();

	}

}
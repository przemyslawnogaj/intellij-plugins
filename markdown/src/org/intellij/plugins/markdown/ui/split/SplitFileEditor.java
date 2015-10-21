package org.intellij.plugins.markdown.ui.split;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.JBSplitter;
import org.intellij.plugins.markdown.settings.MarkdownApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

public abstract class SplitFileEditor extends UserDataHolderBase implements FileEditor {
  public static final Key<SplitFileEditor> PARENT_SPLIT_KEY = Key.create("parentSplit");

  private static final String MY_PROPORTION_KEY = "SplitFileEditor.Proportion";

  @NotNull
  protected final FileEditor myMainEditor;
  @NotNull
  protected final FileEditor mySecondEditor;
  @NotNull
  private final JComponent myComponent;
  @NotNull
  private SplitEditorLayout mySplitEditorLayout = MarkdownApplicationSettings.getInstance().getSplitEditorLayout();
  @NotNull
  private final MyListenersMultimap myListenersGenerator = new MyListenersMultimap();

  private SplitEditorToolbar myToolbarWrapper;

  public SplitFileEditor(@NotNull FileEditor mainEditor, @NotNull FileEditor secondEditor) {
    myMainEditor = mainEditor;
    mySecondEditor = secondEditor;

    myComponent = createComponent();

    if (myMainEditor instanceof TextEditor) {
      myMainEditor.putUserData(PARENT_SPLIT_KEY, this);
    }
    if (mySecondEditor instanceof TextEditor) {
      mySecondEditor.putUserData(PARENT_SPLIT_KEY, this);
    }
  }

  @NotNull
  private JComponent createComponent() {
    final JBSplitter splitter = new JBSplitter(false, 0.5f, 0.15f, 0.85f);
    splitter.setSplitterProportionKey(MY_PROPORTION_KEY);
    splitter.setFirstComponent(myMainEditor.getComponent());
    splitter.setSecondComponent(mySecondEditor.getComponent());



    myToolbarWrapper = new SplitEditorToolbar(splitter);
    if (myMainEditor instanceof TextEditor) {
      myToolbarWrapper.addGutterToTrack(((EditorGutterComponentEx)((TextEditor)myMainEditor).getEditor().getGutter()));
    }
    if (mySecondEditor instanceof TextEditor) {
      myToolbarWrapper.addGutterToTrack(((EditorGutterComponentEx)((TextEditor)mySecondEditor).getEditor().getGutter()));
    }

    final JPanel result = new JPanel(new BorderLayout());
    result.add(myToolbarWrapper, BorderLayout.NORTH);
    result.add(splitter, BorderLayout.CENTER);


    return result;
  }

  @NotNull
  protected AnAction[] createToolbarActions() {
    return AnAction.EMPTY_ARRAY;
  }

  public void triggerLayoutChange() {
    final int oldValue = mySplitEditorLayout.ordinal();
    final int newValue = (oldValue + 1) % SplitEditorLayout.values().length;

    mySplitEditorLayout = SplitEditorLayout.values()[newValue];

    invalidateLayout();
  }

  private void invalidateLayout() {
    myMainEditor.getComponent().setVisible(mySplitEditorLayout.showFirst);
    mySecondEditor.getComponent().setVisible(mySplitEditorLayout.showSecond);
    myToolbarWrapper.adjustSpacing();
    myComponent.repaint();

    IdeFocusManager.findInstanceByComponent(myComponent).requestFocus(myComponent, true);
  }

  @NotNull
  public FileEditor getMainEditor() {
    return myMainEditor;
  }

  @NotNull
  public FileEditor getSecondEditor() {
    return mySecondEditor;
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myMainEditor.getPreferredFocusedComponent();
  }

  @NotNull
  @Override
  public FileEditorState getState(@NotNull FileEditorStateLevel level) {
    return new MyFileEditorState(mySplitEditorLayout.name(), myMainEditor.getState(level), mySecondEditor.getState(level));
  }

  @Override
  public void setState(@NotNull FileEditorState state) {
    if (state instanceof MyFileEditorState) {
      final MyFileEditorState compositeState = (MyFileEditorState)state;
      if (compositeState.getFirstState() != null) {
        myMainEditor.setState(compositeState.getFirstState());
      }
      if (compositeState.getSecondState() != null) {
        mySecondEditor.setState(compositeState.getSecondState());
      }
      if (compositeState.getSplitLayout() != null) {
        mySplitEditorLayout = SplitEditorLayout.valueOf(compositeState.getSplitLayout());
        invalidateLayout();
      }
    }
  }

  @Override
  public boolean isModified() {
    return myMainEditor.isModified() || mySecondEditor.isModified();
  }

  @Override
  public boolean isValid() {
    return myMainEditor.isValid() && mySecondEditor.isValid();
  }

  @Override
  public void selectNotify() {
    myMainEditor.selectNotify();
    mySecondEditor.selectNotify();
  }

  @Override
  public void deselectNotify() {
    myMainEditor.deselectNotify();
    mySecondEditor.deselectNotify();
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    myMainEditor.addPropertyChangeListener(listener);
    mySecondEditor.addPropertyChangeListener(listener);

    final DoublingEventListenerDelegate delegate = myListenersGenerator.addListenerAndGetDelegate(listener);
    myMainEditor.addPropertyChangeListener(delegate);
    mySecondEditor.addPropertyChangeListener(delegate);
  }

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    myMainEditor.removePropertyChangeListener(listener);
    mySecondEditor.removePropertyChangeListener(listener);

    final DoublingEventListenerDelegate delegate = myListenersGenerator.removeListenerAndGetDelegate(listener);
    if (delegate != null) {
      myMainEditor.removePropertyChangeListener(delegate);
      mySecondEditor.removePropertyChangeListener(delegate);
    }
  }

  @Nullable
  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return myMainEditor.getBackgroundHighlighter();
  }

  @Nullable
  @Override
  public FileEditorLocation getCurrentLocation() {
    return myMainEditor.getCurrentLocation();
  }

  @Nullable
  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return myMainEditor.getStructureViewBuilder();
  }

  @Override
  public void dispose() {
    Disposer.dispose(myMainEditor);
    Disposer.dispose(mySecondEditor);
  }

  static class MyFileEditorState implements FileEditorState {
    @Nullable
    private final String mySplitLayout;
    @Nullable
    private final FileEditorState myFirstState;
    @Nullable
    private final FileEditorState mySecondState;

    public MyFileEditorState(@Nullable String splitLayout, @Nullable FileEditorState firstState, @Nullable FileEditorState secondState) {
      mySplitLayout = splitLayout;
      myFirstState = firstState;
      mySecondState = secondState;
    }

    @Nullable
    public String getSplitLayout() {
      return mySplitLayout;
    }

    @Nullable
    public FileEditorState getFirstState() {
      return myFirstState;
    }

    @Nullable
    public FileEditorState getSecondState() {
      return mySecondState;
    }

    @Override
    public boolean canBeMergedWith(FileEditorState otherState, FileEditorStateLevel level) {
      return otherState instanceof MyFileEditorState
             && (myFirstState == null || myFirstState.canBeMergedWith(((MyFileEditorState)otherState).myFirstState, level))
             && (mySecondState == null || mySecondState.canBeMergedWith(((MyFileEditorState)otherState).mySecondState, level));
    }
  }

  private class DoublingEventListenerDelegate implements PropertyChangeListener {
    @NotNull
    private final PropertyChangeListener myDelegate;

    private DoublingEventListenerDelegate(@NotNull PropertyChangeListener delegate) {
      myDelegate = delegate;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      myDelegate.propertyChange(new PropertyChangeEvent(SplitFileEditor.this, evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()));
    }
  }

  private class MyListenersMultimap {
    private final Map<PropertyChangeListener, Pair<Integer, DoublingEventListenerDelegate>> myMap =
      new HashMap<PropertyChangeListener, Pair<Integer, DoublingEventListenerDelegate>>();

    @NotNull
    public DoublingEventListenerDelegate addListenerAndGetDelegate(@NotNull PropertyChangeListener listener) {
      if (!myMap.containsKey(listener)) {
        myMap.put(listener, Pair.create(1, new DoublingEventListenerDelegate(listener)));
      }
      else {
        final Pair<Integer, DoublingEventListenerDelegate> oldPair = myMap.get(listener);
        myMap.put(listener, Pair.create(oldPair.getFirst() + 1, oldPair.getSecond()));
      }

      return myMap.get(listener).getSecond();
    }

    @Nullable
    public DoublingEventListenerDelegate removeListenerAndGetDelegate(@NotNull PropertyChangeListener listener) {
      final Pair<Integer, DoublingEventListenerDelegate> oldPair = myMap.get(listener);
      if (oldPair == null) {
        return null;
      }

      if (oldPair.getFirst() == 1) {
        myMap.remove(listener);
      }
      else {
        myMap.put(listener, Pair.create(oldPair.getFirst() - 1, oldPair.getSecond()));
      }
      return oldPair.getSecond();
    }
  }

  public enum SplitEditorLayout {
    FIRST(true, false),
    SECOND(false, true),
    SPLIT(true, true);

    public final boolean showFirst;
    public final boolean showSecond;

    SplitEditorLayout(boolean showFirst, boolean showSecond) {
      this.showFirst = showFirst;
      this.showSecond = showSecond;
    }
  }
}
/*
 * Copyright (c) 2018 Charlie Waters
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.notes;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@Slf4j
public class NotesPanel extends PluginPanel
{
	private final JEditorPane notesEditor = new JEditorPane();
	private NotesConfig config;
	private String notesCache;

	void init(NotesConfig config)
	{
		this.config = config;

		// this may or may not qualify as a hack
		// but this lets the editor pane expand to fill the whole parent panel
		getParent().setLayout(new BorderLayout());
		getParent().add(this, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));

		final JLabel notesHeader = new JLabel("Notes");
		add(notesHeader, BorderLayout.NORTH);

		notesEditor.setContentType("text/plain");

		// load note text
		notesCache = config.notesData();
		notesEditor.setText(notesCache);

		// add undo manager edit listener
		UndoManager undoManager = new UndoManager();
		notesEditor.getDocument().addUndoableEditListener(new UndoableEditListener()
		{
			@Override
			public void undoableEditHappened(UndoableEditEvent e)
			{
				undoManager.addEdit(e.getEdit());
			}
		});

		// add undo/redo actions
		notesEditor.getActionMap().put("undoAction", new AbstractAction("Undo")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try {
					if (undoManager.canUndo())
					{
						undoManager.undo();
					}
				}
				catch (CannotUndoException ex)
				{

				}
			}
		});
		notesEditor.getActionMap().put("redoAction", new AbstractAction("Redo")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try {
					if (undoManager.canRedo())
					{
						undoManager.redo();
					}
				}
				catch (CannotRedoException ex)
				{

				}
			}
		});

		// add save action
		notesEditor.getActionMap().put("saveAction", new AbstractAction("Save")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				updateNotes();
			}
		});

		// keyboard shortcuts
		notesEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK), "undoAction");
		notesEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK), "redoAction");
		notesEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK), "saveAction");

		// save note on focus loss
		notesEditor.addFocusListener(new FocusListener()
		{
			@Override
			public void focusGained(FocusEvent e)
			{

			}

			@Override
			public void focusLost(FocusEvent e)
			{
				updateNotes();
			}
		});
		add(notesEditor, BorderLayout.CENTER);
	}

	void setNotes(String data)
	{
		notesCache = data;
		notesEditor.setText(notesCache);
	}

	private void updateNotes()
	{
		// get editor text and save to config when changed
		String data = notesEditor.getText();
		if (!data.equals(notesCache))
		{
			notesCache = data;
			config.notesData(notesCache);
		}
	}
}

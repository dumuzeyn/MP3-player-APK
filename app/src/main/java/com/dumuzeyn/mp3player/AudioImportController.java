package com.dumuzeyn.mp3player;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

final class AudioImportController {
    private static final String DEBUG_TAG = "MP3PlayerDebug";
    private static final int PICK_AUDIO = 2001;
    private static final int PICK_AUDIO_FOLDER = 2002;
    private static final int MAX_FOLDER_IMPORT = 3000;
    private static final long MAX_AUDIO_BYTES = 220L * 1024L * 1024L;

    private final MainActivityCore host;
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean closed;

    AudioImportController(MainActivityCore host) {
        this.host = host;
    }

    void openFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        host.startActivityForResult(Intent.createChooser(intent,
                host.tr("Choose music", "Выберите музыку")), PICK_AUDIO);
    }

    void openFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        host.startActivityForResult(Intent.createChooser(intent,
                host.tr("Choose music folder", "Выберите папку с музыкой")), PICK_AUDIO_FOLDER);
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return false;
        }
        final ArrayList<Uri> selectedUris = new ArrayList<>();
        final Uri selectedTree;
        if (requestCode == PICK_AUDIO) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    selectedUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedUris.add(data.getData());
            }
            selectedTree = null;
        } else if (requestCode == PICK_AUDIO_FOLDER && data.getData() != null) {
            selectedTree = data.getData();
        } else {
            return false;
        }
        final int permissionFlags = data.getFlags();
        final HashSet<String> knownUris = new HashSet<>();
        for (Track track : host.tracks) {
            knownUris.add(track.uri);
        }
        try {
            importExecutor.execute(() -> processImport(
                    selectedUris, selectedTree, permissionFlags, knownUris));
        } catch (RejectedExecutionException ignored) {
            return false;
        }
        return true;
    }

    void close() {
        closed = true;
        importExecutor.shutdown();
    }

    private void processImport(ArrayList<Uri> selectedUris, Uri treeUri, int permissionFlags,
            HashSet<String> knownUris) {
        ArrayList<Track> imported = new ArrayList<>();
        if (treeUri != null) {
            importFolder(treeUri, permissionFlags, knownUris, imported);
        } else {
            for (Uri uri : selectedUris) {
                Track track = readTrack(uri, permissionFlags, true, knownUris);
                if (track != null) {
                    imported.add(track);
                    TrackStore.upsert(host, track);
                }
            }
        }
        if (imported.isEmpty() || closed) {
            return;
        }
        host.uiHandler.post(() -> {
            if (closed) {
                return;
            }
            for (Track track : imported) {
                if (host.findTrack(track.uri) == null) {
                    host.tracks.add(track);
                }
            }
            TrackStore.sort(host.tracks);
            host.render();
        });
    }

    private void importFolder(Uri treeUri, int flags, HashSet<String> knownUris,
            ArrayList<Track> importedTracks) {
        if (treeUri == null || !"content".equalsIgnoreCase(treeUri.getScheme())) {
            return;
        }
        int takeFlags = flags & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            host.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
        } catch (RuntimeException error) {
            Log.w(DEBUG_TAG, "persist_folder_permission_failed uri=" + treeUri + " error=" + error.getMessage());
        }
        int[] imported = {0};
        try {
            scanDocumentTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri), imported,
                    knownUris, importedTracks);
        } catch (RuntimeException error) {
            Log.w(DEBUG_TAG, "folder_import_failed uri=" + treeUri + " error=" + error.getMessage());
        }
    }

    private void scanDocumentTree(Uri treeUri, String documentId, int[] imported,
            HashSet<String> knownUris, ArrayList<Track> importedTracks) {
        if (closed || imported[0] >= MAX_FOLDER_IMPORT) {
            return;
        }
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
        Cursor cursor = null;
        try {
            cursor = host.getContentResolver().query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            }, null, null, null);
            while (cursor != null && cursor.moveToNext() && imported[0] < MAX_FOLDER_IMPORT) {
                String childId = cursor.getString(0);
                String mimeType = cursor.getString(1);
                String displayName = cursor.getString(2);
                Uri childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                    scanDocumentTree(treeUri, childId, imported, knownUris, importedTracks);
                } else if (isAudioDocument(mimeType, displayName)) {
                    Track track = readTrack(childUri, 0, false, knownUris);
                    if (track != null) {
                        importedTracks.add(track);
                        TrackStore.upsert(host, track);
                        imported[0]++;
                    }
                }
            }
        } catch (RuntimeException error) {
            Log.w(DEBUG_TAG, "folder_scan_failed uri=" + childrenUri + " error=" + error.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean isAudioDocument(String mimeType, String displayName) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            return true;
        }
        return hasAudioExtension(displayName);
    }

    private Track readTrack(Uri uri, int permissionFlags, boolean persistPermission,
            Set<String> knownUris) {
        if (!isSafeAudioUri(uri)) {
            Log.w(DEBUG_TAG, "add_track_rejected uri=" + uri + " reason=unsafe");
            return null;
        }
        if (persistPermission) {
            int takeFlags = permissionFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            if (takeFlags == 0) {
                takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            }
            try {
                host.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            } catch (RuntimeException error) {
                Log.w(DEBUG_TAG, "persist_permission_failed uri=" + uri + " error=" + error.getMessage());
            }
        }
        String value = uri.toString();
        if (knownUris.contains(value)) {
            return null;
        }
        try {
            String mime = host.getContentResolver().getType(uri);
            String displayName = queryDisplayName(uri);
            long size = querySize(uri);
            boolean canOpen = TrackStore.canOpenForRead(host, uri);
            Log.i(DEBUG_TAG, "add_track_candidate uri=" + uri + " mime=" + mime
                    + " displayName=" + displayName + " size=" + size + " canOpen=" + canOpen);
            if (!canOpen) {
                return null;
            }
            Track track = TrackStore.fromUri(host, uri);
            if (track != null) {
                knownUris.add(value);
                Log.i(DEBUG_TAG, "add_track_saved uri=" + uri + " title=" + track.title
                        + " durationMs=" + track.durationMs);
            }
            return track;
        } catch (RuntimeException error) {
            Log.e(DEBUG_TAG, "add_track_failed uri=" + uri + " error=" + error.getMessage(), error);
            return null;
        }
    }

    private boolean isSafeAudioUri(Uri uri) {
        if (uri == null || !"content".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        try {
            String type = host.getContentResolver().getType(uri);
            boolean extensionMatches = hasAudioExtension(queryDisplayName(uri));
            if (type != null && !type.toLowerCase(Locale.ROOT).startsWith("audio/") && !extensionMatches) {
                return false;
            }
            if (type == null && !extensionMatches) {
                return false;
            }
            long size = querySize(uri);
            return size <= 0L || size <= MAX_AUDIO_BYTES;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean hasAudioExtension(String displayName) {
        if (displayName == null) {
            return false;
        }
        String lower = displayName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac")
                || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".flac");
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = host.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            return cursor != null && cursor.moveToFirst() ? cursor.getString(0) : uri.getLastPathSegment();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private long querySize(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = host.getContentResolver().query(uri,
                    new String[]{OpenableColumns.SIZE}, null, null, null);
            return cursor != null && cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

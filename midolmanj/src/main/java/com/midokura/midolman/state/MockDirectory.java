/*
 * Copyright 2011 Midokura KK
 */

package com.midokura.midolman.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jute.Record;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NotEmptyException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.OpResult;
import org.apache.zookeeper.KeeperException.*;
import org.apache.zookeeper.proto.CreateRequest;
import org.apache.zookeeper.proto.DeleteRequest;
import org.apache.zookeeper.proto.SetDataRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is an in-memory, naive implementation of the Directory interface.
 * It is only meant to be used in tests. However, it is packaged here
 * so that it can be used by external projects (e.g. functional tests).
 */
public class MockDirectory implements Directory {

    private final static Logger log = LoggerFactory.getLogger(MockDirectory.class);

    private class Node {
        // The node's path from the root.
        private String path;
        private byte[] data;
        CreateMode mode;
        int sequence;
        Map<String, Node> children;
        Set<Runnable> dataWatchers;
        Set<Runnable> childrenWatchers;

        // We currently have no need for Watchers on 'exists'.

        Node(String path, byte[] data, CreateMode mode) {
            super();
            this.path = path;
            this.data = data;
            this.mode = mode;
            this.sequence = 0;
            this.children = new HashMap<String, Node>();
            this.dataWatchers = new HashSet<Runnable>();
            this.childrenWatchers = new HashSet<Runnable>();
        }

        Node getChild(String name) throws NoNodeException {
            Node child = children.get(name);
            if (null == child)
                throw new NoNodeException(path + '/' + name);
            return child;
        }

        // Document that this returns the absolute path of the child
        String addChild(String name, byte[] data, CreateMode mode,
                        boolean multi)
                throws NodeExistsException, NoChildrenForEphemeralsException {
            if (enableDebugLog)
                log.debug("addChild {} => {}", name, data);
            if (!mode.isSequential() && children.containsKey(name))
                throw new NodeExistsException(path + '/' + name);
            if (this.mode.isEphemeral())
                throw new NoChildrenForEphemeralsException(path + '/' + name);
            if (mode.isSequential()) {
                name = name + String.format("%010d", sequence++);
            }

            String childPath = path + "/" + name;
            Node child = new Node(childPath, data, mode);
            children.put(name, child);
            notifyChildrenWatchers(multi);
            return childPath;
        }

        void setData(byte[] data, boolean multi) {
            if (enableDebugLog)
                log.debug("[child]setData => {}", data);
            this.data = data;
            notifyDataWatchers(multi);
        }

        byte[] getData(Runnable watcher) {
            if (watcher != null)
                dataWatchers.add(watcher);
            if (data == null) {
                return null;
            }
            return data.clone();
        }

        Set<String> getChildren(Runnable watcher) {
            if (watcher != null)
                childrenWatchers.add(watcher);
            return new HashSet<String>(children.keySet());
        }

        void deleteChild(String name, boolean multi)
                throws NoNodeException, NotEmptyException {
            Node child = children.get(name);
            String childPath = path + "/" + name;
            if (null == child)
                throw new NoNodeException(childPath);
            if (child.children.size() > 0)
                throw new NotEmptyException(childPath);
            children.remove(name);
            child.notifyDataWatchers(multi);
            this.notifyChildrenWatchers(multi);
        }

        void notifyChildrenWatchers(boolean multi) {
            // Each Watcher is called back at most once for every time they
            // register.
            Set<Runnable> watchers = childrenWatchers;
            childrenWatchers = new HashSet<Runnable>();
            for (Runnable watcher : watchers) {
                if (multi) {
                    multiDataWatchers.add(watcher);
                } else {
                    watcher.run();
                }
            }
        }

        void notifyDataWatchers(boolean multi) {
            // Each Watcher is called back at most once for every time they
            // register.
            Set<Runnable> watchers = dataWatchers;
            dataWatchers = new HashSet<Runnable>();
            for (Runnable watcher : watchers) {
                if (multi) {
                    multiDataWatchers.add(watcher);
                } else {
                    watcher.run();
                }
            }
        }
    }

    private Node rootNode;
    private Set<Runnable> multiDataWatchers;
    public boolean enableDebugLog = false;

    private MockDirectory(Node root, Set<Runnable> multiWatchers) {
        rootNode = root;
        // All the nodes will belong to another MockDirectory whose
        // multiDataWatchers set is initialized, and they will use it.
        multiDataWatchers = multiWatchers;
    }

    public MockDirectory() {
        rootNode = new Node("", null, CreateMode.PERSISTENT);
        multiDataWatchers = new HashSet<Runnable>();
    }

    private Node getNode(String path) throws NoNodeException {
        String[] path_elems = path.split("/");
        return getNode(path_elems, path_elems.length);
    }

    private Node getNode(String[] path, int depth) throws NoNodeException {
        Node curNode = rootNode;
        // TODO(pino): fix this hack - starts at 1 to skip empty string.
        for (int i = 1; i < depth; i++) {
            String path_elem = path[i];
            curNode = curNode.getChild(path_elem);
        }
        if (enableDebugLog)
            log.debug("get {} => {}", path, curNode);
        return curNode;
    }

    private String add(String relativePath, byte[] data, CreateMode mode,
                       boolean multi)
            throws NoNodeException, NodeExistsException,
            NoChildrenForEphemeralsException {
        if (enableDebugLog)
            log.debug("add {} => {}", relativePath, data);
        if (!relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with '/'");
        }
        String[] path = relativePath.split("/", -1);
        if (path.length == 0)
            throw new IllegalArgumentException("Cannot add the root node");
        Node parent = getNode(path, path.length - 1);
        String childPath = parent.addChild(path[path.length - 1], data, mode,
                                           multi);
        return childPath.substring(rootNode.path.length());
    }

    @Override
    public String add(String relativePath, byte[] data, CreateMode mode)
            throws NoNodeException, NodeExistsException,
            NoChildrenForEphemeralsException {
        return add(relativePath, data, mode, false);
    }

    private void update(String path, byte[] data, boolean multi)
        throws NoNodeException {
        getNode(path).setData(data, multi);
    }

    @Override
    public void update(String path, byte[] data) throws NoNodeException {
        update(path, data, false);
    }

    @Override
    public byte[] get(String path, Runnable watcher) throws NoNodeException {
        return getNode(path).getData(watcher);
    }

    @Override
    public Set<String> getChildren(String path, Runnable watcher)
            throws NoNodeException {
        return getNode(path).getChildren(watcher);
    }

    @Override
    public boolean has(String path) {
        try {
            getNode(path);
            return true;
        } catch (NoNodeException e) {
            return false;
        }
    }

    private void delete(String relativePath, boolean multi)
        throws NoNodeException, NotEmptyException {
        String[] path = relativePath.split("/");
        if (path.length == 0)
            throw new IllegalArgumentException("Cannot delete the root node");
        Node parent = getNode(path, path.length - 1);
        parent.deleteChild(path[path.length - 1], multi);
    }

    @Override
    public void delete(String relativePath)
        throws NoNodeException, NotEmptyException {
        delete(relativePath, false);
    }

    @Override
    public Directory getSubDirectory(String path) throws NoNodeException {
        Node subdirRoot = getNode(path);
        return new MockDirectory(subdirRoot, multiDataWatchers);
    }

    @Override
    public List<OpResult> multi(List<Op> ops) throws InterruptedException,
            KeeperException {
        List<OpResult> results = new ArrayList<OpResult>();
        // Fire watchers after finishing multi operation.
        // Copy to the local Set to avoid concurrent access.
        Set<Runnable> watchers = new HashSet<Runnable>();
        try {
            for (Op op : ops) {
                Record record = op.toRequestRecord();
                if (record instanceof CreateRequest) {
                    // TODO(pino, ryu): should we use the try/catch and create
                    // new ErrorResult? Don't for now, but this means that the
                    // unit tests can't purposely make a bad Op.
                    // try {
                    CreateRequest req = CreateRequest.class.cast(record);
                    String path = this.add(req.getPath(), req.getData(),
                                           CreateMode.fromFlag(req.getFlags()),
                                           true);
                    results.add(new OpResult.CreateResult(path));
                    // } catch (KeeperException e) {
                    // e.printStackTrace();
                    // results.add(
                    // new OpResult.ErrorResult(e.code().intValue()));
                    // }
                } else if (record instanceof SetDataRequest) {
                    SetDataRequest req = SetDataRequest.class.cast(record);
                    this.update(req.getPath(), req.getData(), true);
                    // We create the SetDataResult with Stat=null. The
                    // Directory interface doesn't provide the Stat object.
                    results.add(new OpResult.SetDataResult(null));
                } else if (record instanceof DeleteRequest) {
                    DeleteRequest req = DeleteRequest.class.cast(record);
                    this.delete(req.getPath(), true);
                    results.add(new OpResult.DeleteResult());
                } else {
                    // might be CheckVersionRequest or some new type we miss.
                    throw new IllegalArgumentException(
                        "This mock implementation only supports Create, " +
                        "SetData and Delete operations");
                }
            }
        } finally {
            for (Runnable watcher : multiDataWatchers) {
                watchers.add(watcher);
            }
            multiDataWatchers.clear();
        }

        for (Runnable watcher : watchers) {
            watcher.run();
        }

        return results;
    }

    @Override
    public long getSessionId() {
        return 0;
    }

    @Override
    public String toString() {
        return "MockDirectory{" +
            "node.path=" + rootNode .path +
            '}';
    }
}

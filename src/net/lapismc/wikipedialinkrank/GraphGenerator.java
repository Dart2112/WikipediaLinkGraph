package net.lapismc.wikipedialinkrank;

import org.gephi.io.generator.spi.Generator;
import org.gephi.io.generator.spi.GeneratorUI;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.lookup.ServiceProvider;

import java.util.ArrayList;

@ServiceProvider(service = Generator.class)
public class GraphGenerator implements Generator {

    @SuppressWarnings("WeakerAccess")
    protected ProgressTicket progress;
    @SuppressWarnings("WeakerAccess")
    protected boolean cancel = false;
    private ArrayList<Connection> connections;

    void setConnections(ArrayList<Connection> connections) {
        this.connections = connections;
    }

    public void generate(ContainerLoader container) {
        for (Connection c : connections) {
            if (c.getWeight() == 1) {
                continue;
            }
            // create nodes if they don't already exist
            NodeDraft a;
            NodeDraft b;
            if (container.nodeExists(c.getTitleA())) {
                a = container.getNode(c.getTitleA());
            } else {
                a = container.factory().newNodeDraft(c.getTitleA());
                a.setLabel(c.getTitleA());
            }
            if (container.nodeExists(c.getTitleB())) {
                b = container.getNode(c.getTitleB());
            } else {
                b = container.factory().newNodeDraft(c.getTitleB());
                b.setLabel(c.getTitleB());
            }

            // create edge
            EdgeDraft e = container.factory().newEdgeDraft();
            e.setSource(a);
            e.setTarget(b);
            e.setWeight(c.getWeight());

            // add nodes and edge to graph if they aren't already added
            if (!container.nodeExists(a.getLabel())) {
                container.addNode(a);
            }
            if (!container.nodeExists(b.getLabel())) {
                container.addNode(b);
            }
            container.addEdge(e);
        }
    }

    public String getName() {
        return "Wikipedia Links";
    }

    public GeneratorUI getUI() {
        return null;
    }

    public boolean cancel() {
        cancel = true;
        return true;
    }

    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }

}

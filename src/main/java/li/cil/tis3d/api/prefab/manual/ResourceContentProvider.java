package li.cil.tis3d.api.prefab.manual;

import com.google.common.base.Charsets;
import li.cil.tis3d.api.manual.ContentProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Basic implementation of a content provider based on Minecraft's resource
 * loading framework.
 * <p>
 * Beware that the manual is unaware of resource domains. In other words, two
 * paths that are identical except for their resource domain will be the same,
 * as seen from the manual. This means you should probably place your
 * documentation somewhere other than <tt>doc/</tt>, because that's where the
 * OpenComputers documentation lives, and it is queried first - meaning if you
 * have a page with the same path as one in OpenComputers, it is practically
 * unreachable (because the OC provider is always queried first).
 */
@SuppressWarnings("UnusedDeclaration")
public class ResourceContentProvider implements ContentProvider {
    private final String resourceDomain;

    private final String basePath;

    public ResourceContentProvider(final String resourceDomain, final String basePath) {
        this.resourceDomain = resourceDomain;
        this.basePath = basePath;
    }

    public ResourceContentProvider(final String resourceDomain) {
        this(resourceDomain, "");
    }

    @Override
    @Nullable
    public Iterable<String> getContent(final String path) {
        final Identifier location = new Identifier(resourceDomain, basePath + (path.startsWith("/") ? path.substring(1) : path));
        try (final InputStream is = MinecraftClient.getInstance().getResourceManager().getResource(location).getInputStream()) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            final ArrayList<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (final Throwable ignored) {
            return null;
        }
    }
}

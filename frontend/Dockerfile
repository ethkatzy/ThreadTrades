# Production image: `next build` then `next start`. Dockerfile.dev (next dev
# against a bind-mounted source tree) is for local Docker Compose only --
# this is what Render deploys.
#
# NEXT_PUBLIC_* vars are inlined into the client bundle at build time, not
# read at container start -- they must be declared as ARGs here so Render's
# Docker build forwards its service env vars of the same name in as build
# args (Render does this automatically for any ARG whose name matches a
# configured env var). Changing one of these later requires a rebuild, not
# just a restart. BACKEND_INTERNAL_URL isn't a NEXT_PUBLIC_* var (it's read
# server-side, not by the browser) but needs the same ARG treatment: `next
# build` resolves next.config.ts's rewrites() destination once and bakes it
# into .next/routes-manifest.json -- `next start` does NOT re-read
# process.env for it at container start, so without this ARG it would
# silently fall back to the http://localhost:8080 default forever (see
# ROADMAP.md #3).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_STORAGE_PUBLIC_URL
ARG BACKEND_INTERNAL_URL
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/package.json /app/package-lock.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
COPY --from=build /app/next.config.ts ./

# next start reads $PORT itself; Render sets it at runtime.
EXPOSE 3000
CMD ["npm", "run", "start"]

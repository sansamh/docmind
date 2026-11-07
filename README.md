# DocMind — RAG Knowledge Base Q&A

Full-stack **Retrieval-Augmented Generation (RAG)** application: upload documents (PDF, DOCX, TXT, MD, CSV, JSON), and chat with them — every answer streams in real time and cites the exact source chunks it was built from.

Live demo: *coming soon*

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen) ![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-green) ![React](https://img.shields.io/badge/React-18-blue) ![TypeScript](https://img.shields.io/badge/TypeScript-5-blue)

## Features

- **Document ingestion pipeline** — async processing: parse → semantic chunking (token-based) → embedding → vector store, with per-document status tracking (`PROCESSING / READY / FAILED`)
- **Cited streaming chat** — answers stream token-by-token over SSE; retrieved chunks are sent first as a separate `sources` event and rendered as expandable citation cards with similarity scores
- **Scoped retrieval** — vector search filtered per knowledge base, so answers only draw from that base's documents
- **Grounded prompting** — the model is instructed to answer *only* from retrieved context and cite sources; it admits when the documents don't contain the answer
- **JWT auth** — register/login with BCrypt password hashing, stateless token auth, per-user data isolation
- **Chat history** — messages (with sources) persisted per knowledge base and restored on page load

## Architecture

```
┌─────────────┐  SSE / REST   ┌────────────────────────────────────────┐
│  React 18    │◄────────────►│  Spring Boot 3.5                       │
│  TypeScript  │              │                                        │
│  Vite        │              │  ┌──────────┐   ┌──────────────────┐   │
│  Tailwind 4  │              │  │ Documents │──►│ Ingestion (async) │   │
└─────────────┘              │  └──────────┘   │  parse→chunk→embed │   │
                             │  ┌──────────┐   └────────┬─────────┘   │
                             │  │ RAG chat  │◄── retrieve ┘            │
                             │  └────┬─────┘                          │
                             │       ▼                                │
                             │  H2 / PostgreSQL    VectorStore        │
                             │  (JPA metadata)     (SimpleVectorStore,│
                             │                      swap→pgvector)    │
                             └────────────┬───────────────────────────┘
                                          ▼
                              OpenAI-compatible LLM + Embedding API
```

**Key design point:** all AI access goes through Spring AI's `ChatClient` and `VectorStore` interfaces, so the vector store is swappable — the bundled `SimpleVectorStore` (file-backed, zero infrastructure) is used for local dev, and `PgVectorStore` (Postgres + pgvector) for production without touching business code. Likewise, any OpenAI-compatible endpoint works (OpenAI, GLM, SiliconFlow, …) via base-url config.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring AI 1.0 (ChatClient, TokenTextSplitter, VectorStore), Spring Security + JJWT, JPA |
| Parsers | Apache PDFBox (PDF), Apache POI (DOCX), plain text |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS 4, React Router, Axios, native SSE streaming (fetch + ReadableStream) |
| Storage | H2 (dev) / PostgreSQL (prod), file-backed vector store (dev) / pgvector (prod) |

## Running locally

Prerequisites: Java 21, Node 18+.

**Backend** (from `backend/`):

```bash
export OPENAI_API_KEY=sk-...          # any OpenAI-compatible key
export OPENAI_BASE_URL=https://api.openai.com   # optional, e.g. SiliconFlow/Gendpoint
export LLM_MODEL=gpt-4o-mini
export EMBEDDING_MODEL=text-embedding-3-small
mvn spring-boot:run
```

**Frontend** (from `frontend/`):

```bash
npm install
npm run dev        # http://localhost:5173, proxies /api to :8080
```

## API overview

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` · `/api/auth/login` | JWT auth |
| GET/POST/DELETE | `/api/kb` | knowledge base CRUD |
| GET/POST/DELETE | `/api/kb/{id}/documents` | upload (`multipart`), list, delete |
| POST | `/api/kb/{id}/chat` | question → SSE stream (`sources`, `token`, `done` events) |
| GET | `/api/kb/{id}/chat/messages` | chat history |

## Deployment

- **Frontend** → Vercel (build `npm run build`, output `dist/`)
- **Backend** → Railway or Fly.io (Docker) with a Postgres add-on; switch the vector store bean to `PgVectorStore` and set `spring.profiles.active=prod`

See `backend/Dockerfile` and `vercel.json` for ready-to-use configs.

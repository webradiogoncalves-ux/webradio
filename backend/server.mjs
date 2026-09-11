import express from 'express';import cors from 'cors';import {spawn} from 'node:child_process';import fs from 'node:fs';import path from 'node:path';
const app=express();app.use(cors());app.use(express.json());
const PORT=process.env.PORT||8787;const PROTO=process.env.CASTER_PROTOCOL||'http';const HOST=process.env.CASTER_HOST||'sapircast.caster.fm';const CASTER_PORT=process.env.CASTER_PORT||'19513';const PRIVATE=process.env.CASTER_PRIVATE_TOKEN||'';
const ROOT=path.resolve(new URL('.', import.meta.url).pathname,'..');
const AUTO_SCRIPT=path.join(ROOT,'automation','engine.py');
const AUTO_STATE=path.join(ROOT,'automation','status.json');
let autoProc=null;
const base=`${PROTO}://${HOST}:${CASTER_PORT}`;
async function caster(path,opts={}){const r=await fetch(base+path,{...opts,headers:{Accept:'application/json',...(opts.headers||{})}});const text=await r.text();let data;try{data=JSON.parse(text)}catch{data={raw:text}};if(!r.ok)throw new Error(`Caster ${r.status}: ${data?.error?.message||text}`);return data;}
function authHeaders(){if(!PRIVATE)throw new Error('CASTER_PRIVATE_TOKEN não configurado');return {Authorization:`Bearer ${PRIVATE}`};}
app.get('/api/status',async(req,res)=>{try{const d=await caster('/admin/publicstats.json');const root=Array.isArray(d)?d[0]:d;let source=null;if(Array.isArray(d)){for(const x of d){if(x?.mount||x?.title||x?.listeners||x?.listenurl){source=x;break}}}source=source||root||{};const listeners=source.listeners??source.listener_connections??source.clients??0;const bitrate=source.bitrate??source.quality??'--';const title=source.title??source.songtitle??source.name??'--';res.json({online:true,listeners,bitrate,title,raw:d});}catch(e){res.status(200).json({online:false,listeners:0,bitrate:'--',title:'--',error:e.message});}});
app.post('/api/server/start',async(req,res)=>{try{if(!PRIVATE)return res.status(503).json({success:false,error:'Configure CASTER_PRIVATE_TOKEN no backend.'});res.json(await caster('/private/server/start',{method:'POST',headers:authHeaders()}));}catch(e){res.status(500).json({success:false,error:e.message});}});
app.post('/api/server/stop',async(req,res)=>{try{if(!PRIVATE)return res.status(503).json({success:false,error:'Configure CASTER_PRIVATE_TOKEN no backend.'});res.json(await caster('/private/server/stop',{method:'POST',headers:authHeaders()}));}catch(e){res.status(500).json({success:false,error:e.message});}});

function automationEnv(){return {...process.env,CASTER_HOST:HOST,CASTER_PORT:String(CASTER_PORT),CASTER_MOUNT:process.env.CASTER_MOUNT||'QKnuH',CASTER_SOURCE_PASSWORD:process.env.CASTER_SOURCE_PASSWORD||'',CASTER_SOURCE_USER:process.env.CASTER_SOURCE_USER||'source',CASTER_BITRATE:process.env.CASTER_BITRATE||'96k'};}
function startAutomation(){if(autoProc&&!autoProc.killed)return;autoProc=spawn(process.env.PYTHON_BIN||'python3',[AUTO_SCRIPT],{cwd:ROOT,env:automationEnv(),stdio:['ignore','ignore','pipe']});autoProc.on('exit',()=>{autoProc=null;});}
function stopAutomation(){if(autoProc){autoProc.kill('SIGTERM');autoProc=null;}}
app.get('/api/automation/status',(req,res)=>{try{const d=JSON.parse(fs.readFileSync(AUTO_STATE,'utf8'));d.process=!!autoProc;res.json(d);}catch{res.json({running:!!autoProc,programa:'--',error:null});}});
app.post('/api/automation/start',(req,res)=>{if(!process.env.CASTER_SOURCE_PASSWORD)return res.status(503).json({success:false,error:'Configure CASTER_SOURCE_PASSWORD no backend.'});startAutomation();res.json({success:true,running:true});});
app.post('/api/automation/stop',(req,res)=>{stopAutomation();res.json({success:true,running:false});});
app.post('/api/automation/next',(req,res)=>{if(!process.env.CASTER_SOURCE_PASSWORD)return res.status(503).json({success:false,error:'Configure CASTER_SOURCE_PASSWORD no backend.'});startAutomation();res.json({success:true,message:'O motor continuará a grade automática; para trocar manualmente, use o próximo arquivo pela grade.'});});
app.get('/health',(req,res)=>res.json({ok:true,service:'Painel Web Rádio'}));
app.listen(PORT,()=>console.log(`Painel backend em http://localhost:${PORT}`));

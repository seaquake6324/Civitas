package com.seaquake6324.civitas.domain.security;
import com.seaquake6324.civitas.domain.ChunkCoordinate;import java.util.*;
/** Bounded cardinal search for distance from a claimed chunk to the city border. */
public final class SecurityTopology{
 public static Result borderDistance(Set<Long>territory,long origin,int visitLimit){if(visitLimit<1)throw new IllegalArgumentException("visit limit");if(!territory.contains(origin))return new Result(0,0,false);ArrayDeque<Node>queue=new ArrayDeque<>();Set<Long>visited=new HashSet<>();queue.add(new Node(origin,0));visited.add(origin);int count=0;while(!queue.isEmpty()&&count<visitLimit){Node node=queue.removeFirst();count++;ChunkCoordinate c=ChunkCoordinate.unpack(node.chunk);long[]neighbors={ChunkCoordinate.pack(c.x()+1,c.z()),ChunkCoordinate.pack(c.x()-1,c.z()),ChunkCoordinate.pack(c.x(),c.z()+1),ChunkCoordinate.pack(c.x(),c.z()-1)};for(long next:neighbors){if(!territory.contains(next))return new Result(node.distance,count,false);if(visited.add(next))queue.addLast(new Node(next,node.distance+1));}}return new Result(0,count,!queue.isEmpty());}
 public record Result(int distance,int visited,boolean limited){}private record Node(long chunk,int distance){}private SecurityTopology(){}
}
